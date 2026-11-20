      *================================================================*
      * CBOPERAC - AREA DE COMUNICACION ENTRE JAVA (JNA) Y COBOL      *
      *                                                                *
      * Todos los campos son tipo DISPLAY (texto ASCII) para que      *
      * JNA pueda mapearlos como arreglos de bytes de longitud fija.  *
      * Los importes van en centavos:  PIC 9(n)V99  =>  n+2 caracteres*
      * Ejemplo:  1500.50  ->  "000000000150050"                      *
      *================================================================*
       01  LS-OPERACION.
           05  LS-OPERACION-ID      PIC X(2).
      *    01=DEPOSITO 02=RETIRO 03=TRANSFERENCIA 04=SALDO 05=CIERRE
           05  LS-CUENTA-ORIGEN     PIC X(12).
           05  LS-CUENTA-DESTINO    PIC X(12).
           05  LS-TITULAR           PIC X(40).
           05  LS-TIPO-CUENTA       PIC X(1).
      *    A=AHORROS  C=CORRIENTE
           05  LS-SALDO-IN          PIC 9(13)V99.
           05  LS-SALDO-DEST-IN     PIC 9(13)V99.
           05  LS-IMPORTE           PIC 9(11)V99.
           05  LS-SALDO-OUT         PIC 9(13)V99.
           05  LS-SALDO-DEST-OUT    PIC 9(13)V99.
           05  LS-COMISION          PIC 9(7)V99.
           05  LS-RETCODE           PIC X(2).
      *    00=OK 02=IMPORTE INVALIDO 03=FONDOS INSUFICIENTES
      *    04=LIMITE/REGLA DE NEGOCIO 09=ERROR GENERAL
           05  LS-MENSAJE           PIC X(80).
           05  LS-REF-EXTERNA       PIC X(12).
