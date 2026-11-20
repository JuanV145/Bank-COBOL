# COBOLBank — Transacciones bancarias con COBOL (DLL en Windows, `.so` en Linux)

Sistema de transacciones bancarias con **persistencia de información** donde las
operaciones de negocio están implementadas en **COBOL (GnuCOBOL)**, compiladas
en un **DLL en Windows** o una **biblioteca `.so` en Linux**, e invocadas desde
**Java (Spring Boot) vía JNA**. La interfaz gráfica está desarrollada en
**Angular** y gestiona **timeouts** de las llamadas al runtime COBOL.

```
┌──────────┐   HTTP/JSON   ┌───────────────┐    JNA (byte[] 243)   ┌────────────────────┐
│  Angular  │ ───────────▶ │  Spring Boot   │ ───────────────────▶ │ COBOLBANK.dll      │
│  :4200    │              │  :8080 + SQLite│ ◀─────────────────── │ o libCOBOLBANK.so  │
└──────────┘               └───────────────┘                       └────────────────────┘
                                   │ persistencia
                                   ▼
                              banco.db (SQLite)
```

## Contenido

| Carpeta | Descripción |
|---|---|
| `cobol/` | Los 5 programas COBOL + `copy/CBOPERAC.cpy` (área de comunicación) |
| `backend/` | API REST en Spring Boot (Java 21) con arquitectura MVC + Lombok + SQLite |
| `frontend/` | Interfaz gráfica Angular 20 |
| `scripts/` | `.bat` para Windows y `.sh` para Linux: compilar COBOL y arrancar backend/frontend |
| `dll/` | `COBOLBANK.dll` (Windows) o `libCOBOLBANK.so` (Linux), generado por el script |
| `tools/` | Apache Maven incluido para no requerir instalación extra |

## Inicio rápido en Fedora

```bash
sudo dnf install -y java-21-openjdk-devel maven gcc gnucobol nodejs npm

bash scripts/build_dll.sh     # genera dll/libCOBOLBANK.so
bash scripts/run_backend.sh   # terminal 1 → http://localhost:8080
bash scripts/run_frontend.sh  # terminal 2 → http://localhost:4200
```

Los scripts se pueden ejecutar con `bash` aunque no tengan permiso de ejecución;
si prefieres `./scripts/build_dll.sh`, primero `chmod +x scripts/*.sh`.

> **No copies binarios entre sistemas operativos.** `dll/*.dll`, `dll/*.so`,
> `obj/`, `frontend/node_modules`, `backend/target` y `frontend/dist` son
> específicos de cada máquina. En Fedora hay que generar el `.so` con
> `build_dll.sh` y reinstalar las dependencias de Node con `npm install`.

## Arquitectura MVC del backend (con Lombok)

El backend sigue el patrón **Modelo-Vista-Controlador** en tres paquetes.
**Lombok** se usa solo en las entidades JPA; los DTO son `record` de Java.

```
backend/src/main/java/com/banco/
├── BancoApplication.java        ← arranque, CORS y cuentas demo
├── modelo/                      ← MODELO: datos, persistencia y reglas
│   ├── Cuenta.java
│   ├── Transaccion.java
│   ├── CuentaRepository.java
│   ├── TransaccionRepository.java
│   ├── BancoService.java        ← lógica de negocio y transacciones
│   ├── TipoOperacion.java
│   ├── Errores.java             ← errores de negocio e integración
│   ├── CobolArea.java           ← espejo del COPY CBOPERAC.cpy
│   └── CobolGateway.java        ← JNA + timeout + resultado COBOL
├── vista/
│   └── ApiDtos.java             ← DTOs de entrada/salida (records)
└── controlador/
    ├── BancoController.java     ← endpoints REST
    └── ApiErrors.java           ← excepciones → ApiError
```

**Flujo de una petición:**
`BancoController` recibe HTTP → `BancoService` valida y aplica las reglas →
`CobolGateway` ejecuta el COBOL del DLL con timeout → los repositorios
persisten en SQLite → `ApiDtos` devuelve la vista JSON.
`ApiErrors` normaliza cualquier error en un `ApiError`.

## Las 5 operaciones COBOL (mínimo requerido)

| # | Programa | Operación | Reglas implementadas en COBOL |
|---|---|---|---|
| 1 | `PGDEPOSIT` | Depósito | Importe > 0, máximo $10.000.000; suma al saldo |
| 2 | `PGRETIRO` | Retiro | Importe > 0, máximo $2.000.000, valida fondos suficientes |
| 3 | `PGTRAN` | Transferencia | Cuentas distintas, comisión 1% (mín $2, máx $50), valida fondos incluyendo comisión |
| 4 | `PGSALDO` | Consulta de saldo | Devuelve el saldo disponible |
| 5 | `PGCIERRE` | Cierre de cuenta | Solo si el saldo es $0; la cuenta queda CERRADA |

Códigos de retorno (`LS-RETCODE`): `00` OK · `02` importe inválido ·
`03` fondos insuficientes · `04` regla de negocio · `09` error general.

## Paso 1 — Compilar la biblioteca COBOL

### Windows (MSYS2 + GnuCOBOL)

Instala MSYS2 y GnuCOBOL (una sola vez):

```bat
winget install MSYS2.MSYS2
C:\msys64\usr\bin\bash -lc "pacman -S --noconfirm mingw-w64-ucrt-x86_64-gnucobol"
```

Compila los 5 programas dentro del DLL:

```bat
scripts\build_dll.bat
```

### Linux (Fedora)

```bash
sudo dnf install -y gcc gnucobol
bash scripts/build_dll.sh
```

El script genera `dll/libCOBOLBANK.so` y enlaza con el `libcob` del sistema.
No hay que copiar DLLs: `dnf` deja el runtime instalado. Si el paquete no
expone el enlace `libcob.so`, el script usa la ruta real de `libcob.so.4` que
reporta `ldconfig`. La biblioteca se debe compilar **en la misma máquina Linux**
donde se ejecuta el backend (no vale copiarla desde Windows ni desde otra
distribución).

### Qué hace el script (en ambos sistemas)

1. **Compila** cada `.cbl` a objeto con GnuCOBOL 3.2:
   ```
   cobc -m -c -A -fPIC -o obj/PGDEPOSIT.o cobol/PGDEPOSIT.cbl -I cobol/copy   (x5 programas)
   ```
2. **Enlaza** los 5 objetos en una sola biblioteca con los 5 entry points
   exportados:
   ```
   Windows: gcc -shared -Wl,--export-all-symbols -o dll\COBOLBANK.dll obj\*.o -lcob
   Linux:   gcc -shared -o dll/libCOBOLBANK.so obj/*.o <ruta de libcob>
   ```
3. En Windows **copia** además `libcob-4.dll` y sus dependencias desde
   `C:\msys64\ucrt64\bin` hacia `dll\`. En Linux no hace falta porque el
   runtime queda instalado en el sistema.

> En ambos casos JNA busca el nombre lógico `COBOLBANK`: en Windows resuelve
> `COBOLBANK.dll` y en Linux `libCOBOLBANK.so`.

## Paso 2 — Backend (Spring Boot)

**Windows:**

```bat
scripts\run_backend.bat
```

**Linux:**

```bash
bash scripts/run_backend.sh
```

También sirve, desde la raíz, `mvn -f backend/pom.xml spring-boot:run`
(o `tools/apache-maven/bin/mvn`). Arranca en **http://localhost:8080**.

Al primer arranque se crean en `banco.db` (SQLite) las cuentas demo:

| Cuenta | Titular | Tipo | Saldo |
|---|---|---|---|
| 001000000001 | ANA TORRES | Ahorros | $1.500,00 |
| 001000000002 | BETO RUIZ | Corriente | $250,75 |
| 001000000003 | CARLA DIAZ | Ahorros | $0,00 |

Si la biblioteca COBOL aún no está compilada, el backend arranca igual y las
operaciones responden **503 "BIBLIOTECA COBOL NO DISPONIBLE"** con
instrucciones; el resto de la API (cuentas, historial) funciona.

`run_backend.sh` y `run_backend.bat` fijan el directorio de trabajo en la raíz
del proyecto, para que `banco.db` y `dll/` se resuelvan siempre en el mismo
lugar, sin importar cómo se invoque Maven.

## Paso 3 — Frontend (Angular)

**Windows:**

```bat
scripts\run_frontend.bat
```

**Linux:**

```bash
bash scripts/run_frontend.sh
```

(o `npm start` dentro de `frontend/`). Si vienes de Windows, **borra
`frontend/node_modules` antes de arrancar en Linux**: contiene binarios nativos
de Windows (`esbuild`, `rollup`) y hay que reinstalarlos. Abre
**http://localhost:4200**.

- **Operaciones:** pestañas para las 5 operaciones, selector de cuenta(s),
  importe y un **selector de timeout** (1 s / 3 s / 5 s / 10 s) que se envía por
  petición: la GUI gestiona el timeout de cada llamada COBOL.
- **Cuentas:** listado y creación de cuentas.
- El historial muestra cada transacción con su resultado **APROBADA /
  RECHAZADA / TIMEOUT**, referencia, retCode y mensaje devuelto por COBOL.

## API REST

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/cuentas` | Lista las cuentas |
| POST | `/api/cuentas` | Crea una cuenta `{numero, titular, tipo}` |
| GET | `/api/cuentas/{numero}` | Consulta una cuenta |
| GET | `/api/cuentas/{numero}/transacciones` | Historial de la cuenta |
| POST | `/api/operaciones/deposito` | `{cuentaOrigen, importe, timeoutMs?}` |
| POST | `/api/operaciones/retiro` | `{cuentaOrigen, importe, timeoutMs?}` |
| POST | `/api/operaciones/transferencia` | `{cuentaOrigen, cuentaDestino, importe, timeoutMs?}` |
| POST | `/api/operaciones/saldo` | `{cuentaOrigen, timeoutMs?}` |
| POST | `/api/operaciones/cierre` | `{cuentaOrigen, timeoutMs?}` |
| GET | `/api/transacciones?limite=20` | Historial general |

Ejemplo:

```bash
curl -X POST http://localhost:8080/api/operaciones/transferencia \
  -H "Content-Type: application/json" \
  -d "{\"cuentaOrigen\":\"001000000001\",\"cuentaDestino\":\"001000000002\",\"importe\":100000,\"timeoutMs\":3000}"
```

## Gestión de timeouts (cómo funciona)

1. La **GUI** elige el timeout por operación y lo envía como `timeoutMs`
   (clamp del backend: 500 ms – 15.000 ms).
2. `CobolGateway` ejecuta cada llamada al DLL en un **hilo dedicado** del
   runtime COBOL y espera con `Future.get(timeout)`.
3. Si el programa COBOL no responde:
   - se abandona la espera (la app **nunca se bloquea**),
   - la transacción se persiste en SQLite como **TIMEOUT** (`noRollbackFor`),
   - la API responde **503** y la GUI lo muestra en rojo.
4. Cada llamada devuelve además la **duración real** (`duracionMs`), visible
   en la interfaz.

## Detalle técnico del puente JNA ↔ COBOL

- `cobol/copy/CBOPERAC.cpy` define la **LINKAGE SECTION**: 243 bytes con
  campos PIC X y PIC 9(n)V99 (importes en centavos, texto ASCII).
- El backend la replica en `CobolArea.java` (mismos offsets) y JNA pasa un
  puntero `Memory` al entry point (`PROGRAM-ID`) de cada programa.
- Antes de invocar, se inicializa el runtime GnuCOBOL con `cob_init(0, null)`:
  en Windows se carga `libcob` / `libcob-4` (`.dll`); en Linux se carga `cob`
  (`libcob.so`) con respaldo en las rutas habituales de `libcob.so.4`.
- En Linux, `cob_init` registra sus propios manejadores de `SIGSEGV`/`SIGBUS`
  y sobrescribe los del JVM. Como el JIT usa `SIGSEGV` para los chequeos de
  nulo, `CobolGateway` guarda los manejadores previos con `sigaction` y los
  restaura inmediatamente después de inicializar; sin esto el proceso muere
  al primer acceso nulo. En Windows este conflicto no existe.
- La biblioteca se genera con `cobc -m`, que exporta cada `PROGRAM-ID`
  (`PGDEPOSIT`, `PGRETIRO`, `PGTRAN`, `PGSALDO`, `PGCIERRE`) como función
  `int nombre(char* area)`. En Windows es `COBOLBANK.dll`; en Linux
  `libCOBOLBANK.so`.

## Requisitos

- JDK 21+ (probado con Java 27)
- GnuCOBOL 3.2+ y GCC:
  - Windows: MSYS2 + `mingw-w64-ucrt-x86_64-gnucobol`
  - Fedora: `sudo dnf install -y gcc gnucobol`
- Node.js 22 + npm (Angular 20)
- Maven incluido en `tools/apache-maven` (o cualquier Maven 3.9+)
