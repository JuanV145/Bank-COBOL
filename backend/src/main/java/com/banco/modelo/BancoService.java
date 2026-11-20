package com.banco.modelo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * MODELO - Lógica de negocio. Orquesta cuentas, transacciones y el DLL COBOL
 * dentro de una misma transacción de base de datos.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BancoService {

    private static final long TIMEOUT_MIN = 500;
    private static final long TIMEOUT_MAX = 15_000;

    private final CuentaRepository cuentas;
    private final TransaccionRepository transacciones;
    private final CobolGateway cobol;

    @Value("${cobol.timeout-ms:3000}")
    private long timeoutPorDefecto;

    /** Resultado interno: transacción persistida + respuesta cruda de COBOL. */
    public record Resultado(Transaccion transaccion, CobolGateway.Resultado cobol) {
    }

    @Transactional
    public Cuenta crearCuenta(String numero, String titular, String tipo) {
        if (cuentas.existsById(numero)) {
            throw new Errores.Negocio("La cuenta " + numero + " ya existe");
        }
        return cuentas.save(new Cuenta(numero, titular.toUpperCase(Locale.ROOT),
                tipo.toUpperCase(Locale.ROOT), 0L, "ACTIVA", LocalDateTime.now()));
    }

    public List<Cuenta> listarCuentas() {
        return cuentas.findByOrderByNumeroAsc();
    }

    public Cuenta buscarCuenta(String numero) {
        return cuentas.findById(numero)
                .orElseThrow(() -> new Errores.NoEncontrado("No existe la cuenta " + numero));
    }

    /**
     * Flujo común de las 5 operaciones: validar, llamar al COBOL con timeout
     * y persistir el resultado (incluso si fue rechazada o hubo timeout).
     */
    @Transactional(noRollbackFor = {Errores.Timeout.class, Errores.Cobol.class})
    public Resultado operar(TipoOperacion tipo, String cuentaOrigen, String cuentaDestino,
                            BigDecimal importe, Long timeoutMs, Long simularDemoraMs) {
        long timeout = normalizarTimeout(timeoutMs);
        if (tipo.requiereImporte() && (importe == null || importe.signum() <= 0)) {
            throw new Errores.Negocio("El importe es obligatorio y debe ser mayor que cero");
        }

        Cuenta origen = cargarCuentaActiva(cuentaOrigen);
        Cuenta destino = tipo == TipoOperacion.TRANSFERENCIA ? cargarCuentaActiva(cuentaDestino) : null;

        CobolArea area = new CobolArea();
        String referencia = nuevaReferencia();
        area.putStr(CobolArea.OFF_OPERACION_ID, 2, tipo.codigo());
        area.putStr(CobolArea.OFF_CUENTA_ORIGEN, 12, cuentaOrigen);
        area.putStr(CobolArea.OFF_CUENTA_DESTINO, 12, destino != null ? destino.getNumero() : "");
        area.putStr(CobolArea.OFF_TITULAR, 40, origen.getTitular());
        area.putStr(CobolArea.OFF_TIPO_CUENTA, 1, origen.getTipo());
        area.putMoney(CobolArea.OFF_SALDO_IN, 15, origen.saldo());
        area.putMoney(CobolArea.OFF_SALDO_DEST_IN, 15, destino != null ? destino.saldo() : BigDecimal.ZERO);
        area.putMoney(CobolArea.OFF_IMPORTE, 13, importe != null ? importe : BigDecimal.ZERO);
        area.putStr(CobolArea.OFF_REF_EXTERNA, 12, referencia);

        Transaccion t = new Transaccion();
        t.setReferencia(referencia);
        t.setTipo(tipo.name());
        t.setCuentaOrigen(origen.getNumero());
        t.setCuentaDestino(destino != null ? destino.getNumero() : null);
        t.asignarImporte(importe != null ? importe : BigDecimal.ZERO);
        t.setFechaHora(LocalDateTime.now());

        try {
            CobolGateway.Resultado r = cobol.ejecutar(tipo.programa(), area, timeout, simularDemoraMs);
            t.setRetCode(r.retCode());
            t.setMensaje(truncar(r.mensaje()));
            t.asignarComision(r.comision());
            t.setResultado(r.aprobada() ? "APROBADA" : "RECHAZADA");

            if (r.aprobada()) {
                aplicarEfecto(tipo, origen, destino, r);
                log.info("{} {} aprobada por COBOL ({} ms)", tipo, referencia, r.duracionMs());
            }
            transacciones.save(t);
            return new Resultado(t, r);
        } catch (Errores.Timeout e) {
            t.setResultado("TIMEOUT");
            t.setRetCode("TO");
            t.setMensaje(truncar("SIN RESPUESTA DEL RUNTIME COBOL EN " + timeout + " MS"));
            transacciones.save(t);
            throw e;
        } catch (IllegalStateException e) {
            t.setResultado("RECHAZADA");
            t.setRetCode("99");
            t.setMensaje(truncar("ERROR RUNTIME COBOL: " + e.getMessage()));
            transacciones.save(t);
            throw new Errores.Cobol(e.getMessage(), e);
        }
    }

    private void aplicarEfecto(TipoOperacion tipo, Cuenta origen, Cuenta destino,
                               CobolGateway.Resultado r) {
        if (tipo == TipoOperacion.DEPOSITO || tipo == TipoOperacion.RETIRO) {
            origen.asignarSaldo(r.saldoOrigen());
        } else if (tipo == TipoOperacion.TRANSFERENCIA) {
            origen.asignarSaldo(r.saldoOrigen());
            destino.asignarSaldo(r.saldoDestino());
        } else if (tipo == TipoOperacion.CIERRE_CUENTA) {
            origen.setEstado("CERRADA");
        }
    }

    public List<Transaccion> historial(int limite) {
        return transacciones.findByOrderByIdDesc(PageRequest.of(0, limite)).getContent();
    }

    public List<Transaccion> historialCuenta(String numero) {
        buscarCuenta(numero);
        return transacciones.findTop20ByCuentaOrigenOrCuentaDestinoOrderByIdDesc(numero, numero);
    }

    private Cuenta cargarCuentaActiva(String numero) {
        if (numero == null || numero.isBlank()) {
            throw new Errores.Negocio("El número de cuenta es obligatorio");
        }
        Cuenta cuenta = buscarCuenta(numero.trim());
        if (!"ACTIVA".equals(cuenta.getEstado())) {
            throw new Errores.Negocio("La cuenta " + numero + " está cerrada");
        }
        return cuenta;
    }

    private long normalizarTimeout(Long solicitado) {
        long t = solicitado != null ? solicitado : timeoutPorDefecto;
        return Math.min(TIMEOUT_MAX, Math.max(TIMEOUT_MIN, t));
    }

    /** Referencia externa de hasta 12 caracteres, estilo mainframe. */
    private String nuevaReferencia() {
        long ahora = System.currentTimeMillis();
        int azar = ThreadLocalRandom.current().nextInt(100);
        String ref = "T" + Long.toString(ahora, 36).toUpperCase(Locale.ROOT) + String.format("%02d", azar);
        return ref.length() > 12 ? ref.substring(ref.length() - 12) : ref;
    }

    private String truncar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() > 120 ? texto.substring(0, 120) : texto;
    }
}
