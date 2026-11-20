package com.banco.modelo;

/**
 * MODELO - Errores de negocio e integración. El ControllerAdvice los
 * convierte en la vista ApiError con el código HTTP correspondiente.
 */
public final class Errores {

    private Errores() {
    }

    /** Regla de negocio incumplida (400). */
    public static class Negocio extends RuntimeException {
        public Negocio(String mensaje) {
            super(mensaje);
        }
    }

    /** Cuenta inexistente (404). */
    public static class NoEncontrado extends RuntimeException {
        public NoEncontrado(String mensaje) {
            super(mensaje);
        }
    }

    /** El DLL COBOL no respondió dentro del timeout (503). */
    public static class Timeout extends RuntimeException {
        public Timeout(String programa, long timeoutMs) {
            super("La operación COBOL '" + programa + "' no respondió en " + timeoutMs
                    + " ms (timeout). La transacción fue registrada con estado TIMEOUT.");
        }
    }

    /** El DLL COBOL no está compilado o no se pudo cargar (503). */
    public static class NoDisponible extends RuntimeException {
        public NoDisponible(String mensaje) {
            super(mensaje);
        }
    }

    /** Fallo interno del runtime COBOL (500). */
    public static class Cobol extends RuntimeException {
        public Cobol(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
