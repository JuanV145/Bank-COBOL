package com.banco.modelo;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * MODELO - Puente JNA hacia el DLL COBOL. Serializa las llamadas en un hilo
 * dedicado y aplica timeout para que la API nunca quede bloqueada.
 */
@Component
@Slf4j
public class CobolGateway {

    public interface LibCob extends Library {
        void cob_init(int argc, Pointer argv);
    }

    /** Acceso a libc para preservar los manejadores de señales del JVM. */
    public interface LibC extends Library {
        int sigaction(int senal, Pointer accion, Pointer anterior);
    }

    public interface Operaciones extends Library {
        int PGDEPOSIT(Pointer area);

        int PGRETIRO(Pointer area);

        int PGTRAN(Pointer area);

        int PGSALDO(Pointer area);

        int PGCIERRE(Pointer area);
    }

    /** Resultado normalizado de una llamada al DLL COBOL. */
    public record Resultado(
            String retCode,
            String mensaje,
            BigDecimal saldoOrigen,
            BigDecimal saldoDestino,
            BigDecimal comision,
            long duracionMs
    ) {
        public boolean aprobada() {
            return "00".equals(retCode);
        }

        static Resultado desde(CobolArea area, long duracionMs) {
            return new Resultado(
                    area.getStr(CobolArea.OFF_RETCODE, 2),
                    area.getStr(CobolArea.OFF_MENSAJE, 80),
                    area.getMoney(CobolArea.OFF_SALDO_OUT, 15),
                    area.getMoney(CobolArea.OFF_SALDO_DEST_OUT, 15),
                    area.getMoney(CobolArea.OFF_COMISION, 9),
                    duracionMs
            );
        }
    }

    private final ExecutorService runtime = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cobol-runtime");
        t.setDaemon(true);
        return t;
    });

    @Value("${cobol.library-name:COBOLBANK}")
    private String libraryName;

    @Value("${cobol.library-path:dll}")
    private String libraryPath;

    private Operaciones ops;
    private volatile boolean listo;
    private volatile String errorCarga;

    @PostConstruct
    void inicializar() {
        System.setProperty("jna.library.path", String.join(File.pathSeparator, rutasCandidatas()));
        log.info("jna.library.path={}", System.getProperty("jna.library.path"));

        try {
            LibCob libcob = null;
            String nombreRuntime = null;
            Throwable ultimoError = null;
            for (String candidato : candidatosRuntime()) {
                try {
                    libcob = Native.load(candidato, LibCob.class);
                    nombreRuntime = candidato;
                    break;
                } catch (Throwable e) {
                    ultimoError = e;
                }
            }
            if (libcob == null) {
                throw ultimoError != null ? ultimoError
                        : new IllegalStateException("No se encontró el runtime GnuCOBOL (libcob)");
            }
            iniciarRuntime(libcob);
            log.info("Runtime COBOL '{}' inicializado", nombreRuntime);
            ops = Native.load(libraryName, Operaciones.class);
            listo = true;
            log.info("Biblioteca COBOL '{}' cargada correctamente", libraryName);
        } catch (Throwable e) {
            listo = false;
            errorCarga = e.getMessage();
            log.error("No se pudo cargar el runtime COBOL: {}. Compile la biblioteca con "
                    + "scripts\\build_dll.bat (Windows) o scripts/build_dll.sh (Linux).", errorCarga);
        }
    }

    /**
     * Inicializa el runtime de GnuCOBOL. En Linux, {@code cob_init} registra
     * manejadores propios para SIGSEGV/SIGBUS (y otras señales), sobrescribiendo
     * los del JVM. Como el JIT usa SIGSEGV para los chequeos de nulo, hay que
     * restaurar los manejadores anteriores inmediatamente después de inicializar.
     */
    private void iniciarRuntime(LibCob libcob) {
        if (!Platform.isLinux()) {
            libcob.cob_init(0, null);
            return;
        }
        LibC libc = Native.load(Platform.C_LIBRARY_NAME, LibC.class);
        // SIGHUP, SIGINT, SIGQUIT, SIGBUS, SIGFPE, SIGSEGV, SIGPIPE, SIGTERM, SIGIO
        int[] senales = {1, 2, 3, 7, 8, 11, 13, 15, 29};
        Map<Integer, Memory> previos = new LinkedHashMap<>();
        for (int senal : senales) {
            Memory anterior = new Memory(256);
            if (libc.sigaction(senal, null, anterior) == 0) {
                previos.put(senal, anterior);
            }
        }
        try {
            libcob.cob_init(0, null);
        } finally {
            previos.forEach((senal, anterior) -> libc.sigaction(senal, anterior, null));
        }
    }

    /**
     * Nombres con los que JNA busca el runtime de GnuCOBOL en cada sistema.
     * En Linux JNA antepone "lib" y añade ".so" (cob -> libcob.so).
     */
    private String[] candidatosRuntime() {
        if (Platform.isWindows()) {
            return new String[]{"libcob", "libcob-4"};
        }
        return new String[]{
                "cob",
                "/usr/lib64/libcob.so.4",
                "/usr/lib/x86_64-linux-gnu/libcob.so.4",
                "/usr/local/lib/libcob.so"
        };
    }

    /** Carpetas donde puede estar la biblioteca COBOL y el runtime de GnuCOBOL. */
    private List<String> rutasCandidatas() {
        List<String> rutas = new ArrayList<>();
        agregar(rutas, libraryPath);
        agregar(rutas, "dll");
        agregar(rutas, "backend/dll");
        agregar(rutas, ".");
        if (Platform.isWindows()) {
            agregar(rutas, "C:\\msys64\\mingw64\\bin");
            agregar(rutas, "C:\\msys64\\ucrt64\\bin");
        }
        return rutas;
    }

    private void agregar(List<String> rutas, String carpeta) {
        if (carpeta != null && new File(carpeta).isDirectory() && !rutas.contains(carpeta)) {
            rutas.add(carpeta);
        }
    }

    /**
     * Invoca un programa COBOL con timeout estricto. Cada llamada usa el
     * área de comunicación fija de 243 bytes del COPY CBOPERAC.
     */
    public Resultado ejecutar(String programa, CobolArea area, long timeoutMs, Long simularDemoraMs) {
        if (!listo) {
            throw new Errores.NoDisponible("La biblioteca COBOL '" + libraryName
                    + "' no está disponible. Compile con scripts\\build_dll.bat (Windows) "
                    + "o scripts/build_dll.sh (Linux). Detalle: " + errorCarga);
        }

        Memory memoria = new Memory(CobolArea.SIZE);
        memoria.write(0, area.bytes(), 0, CobolArea.SIZE);

        long inicio = System.nanoTime();
        Future<Integer> futuro = runtime.submit(() -> {
            if (simularDemoraMs != null && simularDemoraMs > 0) {
                try {
                    Thread.sleep(simularDemoraMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Demora simulada interrumpida", e);
                }
            }
            return invocar(programa, memoria);
        });

        try {
            futuro.get(timeoutMs, TimeUnit.MILLISECONDS);
            byte[] salida = new byte[CobolArea.SIZE];
            memoria.read(0, salida, 0, CobolArea.SIZE);
            area.setBytes(salida);
            return Resultado.desde(area, (System.nanoTime() - inicio) / 1_000_000);
        } catch (TimeoutException e) {
            futuro.cancel(true);
            log.error("TIMEOUT: {} no respondió en {} ms", programa, timeoutMs);
            throw new Errores.Timeout(programa, timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Hilo interrumpido esperando al runtime COBOL", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Fallo ejecutando " + programa + ": " + e.getCause(), e);
        }
    }

    private Integer invocar(String programa, Memory memoria) {
        return switch (programa) {
            case "PGDEPOSIT" -> ops.PGDEPOSIT(memoria);
            case "PGRETIRO" -> ops.PGRETIRO(memoria);
            case "PGTRAN" -> ops.PGTRAN(memoria);
            case "PGSALDO" -> ops.PGSALDO(memoria);
            case "PGCIERRE" -> ops.PGCIERRE(memoria);
            default -> throw new IllegalArgumentException("Operación COBOL desconocida: " + programa);
        };
    }

    @PreDestroy
    void destruir() {
        runtime.shutdown();
        try {
            if (!runtime.awaitTermination(2, TimeUnit.SECONDS)) {
                runtime.shutdownNow();
            }
        } catch (InterruptedException e) {
            runtime.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
