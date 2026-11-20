package com.banco.modelo;

/** Las 5 operaciones bancarias implementadas en COBOL. */
public enum TipoOperacion {

    DEPOSITO("PGDEPOSIT", "01"),
    RETIRO("PGRETIRO", "02"),
    TRANSFERENCIA("PGTRAN", "03"),
    CONSULTA_SALDO("PGSALDO", "04"),
    CIERRE_CUENTA("PGCIERRE", "05");

    private final String programa;
    private final String codigo;

    TipoOperacion(String programa, String codigo) {
        this.programa = programa;
        this.codigo = codigo;
    }

    public String programa() {
        return programa;
    }

    public String codigo() {
        return codigo;
    }

    public boolean requiereImporte() {
        return this == DEPOSITO || this == RETIRO || this == TRANSFERENCIA;
    }

    public static TipoOperacion desdeUrl(String tipo) {
        return switch (tipo.toLowerCase()) {
            case "deposito" -> DEPOSITO;
            case "retiro" -> RETIRO;
            case "transferencia" -> TRANSFERENCIA;
            case "saldo", "consulta" -> CONSULTA_SALDO;
            case "cierre" -> CIERRE_CUENTA;
            default -> throw new IllegalArgumentException("Tipo de operación no soportada: " + tipo);
        };
    }
}
