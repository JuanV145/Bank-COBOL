package com.banco.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * MODELO - Espejo en Java del COPY CBOPERAC.cpy.
 *
 * Todos los campos son texto ASCII de longitud fija; los importes viajan
 * en centavos sin punto decimal: 1500.50 -> "000000000150050".
 */
public final class CobolArea {

    public static final int SIZE = 243;

    public static final int OFF_OPERACION_ID = 0;      // PIC X(2)
    public static final int OFF_CUENTA_ORIGEN = 2;     // PIC X(12)
    public static final int OFF_CUENTA_DESTINO = 14;   // PIC X(12)
    public static final int OFF_TITULAR = 26;          // PIC X(40)
    public static final int OFF_TIPO_CUENTA = 66;      // PIC X(1)
    public static final int OFF_SALDO_IN = 67;         // PIC 9(13)V99
    public static final int OFF_SALDO_DEST_IN = 82;    // PIC 9(13)V99
    public static final int OFF_IMPORTE = 97;          // PIC 9(11)V99
    public static final int OFF_SALDO_OUT = 110;       // PIC 9(13)V99
    public static final int OFF_SALDO_DEST_OUT = 125;  // PIC 9(13)V99
    public static final int OFF_COMISION = 140;        // PIC 9(7)V99
    public static final int OFF_RETCODE = 149;         // PIC X(2)
    public static final int OFF_MENSAJE = 151;         // PIC X(80)
    public static final int OFF_REF_EXTERNA = 231;     // PIC X(12)

    private static final Charset CHARSET = StandardCharsets.ISO_8859_1;

    private final byte[] datos = new byte[SIZE];

    public byte[] bytes() {
        return datos;
    }

    public void setBytes(byte[] nuevos) {
        System.arraycopy(nuevos, 0, this.datos, 0, SIZE);
    }

    /** Escribe una cadena en un campo PIC X(n) con relleno de espacios. */
    public void putStr(int offset, int longitud, String valor) {
        String v = valor == null ? "" : valor;
        byte[] src = v.getBytes(CHARSET);
        for (int i = 0; i < longitud; i++) {
            datos[offset + i] = i < src.length ? src[i] : (byte) ' ';
        }
    }

    /** Lee un campo PIC X(n) quitando espacios finales. */
    public String getStr(int offset, int longitud) {
        return new String(datos, offset, longitud, CHARSET).stripTrailing();
    }

    /** Escribe un importe en un campo PIC 9(n)V99 (centavos, ceros a la izquierda). */
    public void putMoney(int offset, int longitud, BigDecimal valor) {
        BigDecimal v = valor == null ? BigDecimal.ZERO : valor;
        long centavos = v.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        if (centavos < 0) {
            centavos = 0;
        }
        String s = String.format("%0" + longitud + "d", centavos);
        if (s.length() > longitud) {
            s = s.substring(s.length() - longitud);
        }
        putStr(offset, longitud, s);
    }

    /** Lee un campo PIC 9(n)V99 y lo devuelve como BigDecimal con 2 decimales. */
    public BigDecimal getMoney(int offset, int longitud) {
        String s = new String(datos, offset, longitud, CHARSET).trim();
        if (s.isEmpty() || !s.chars().allMatch(Character::isDigit)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Long.parseLong(s), 2);
    }
}
