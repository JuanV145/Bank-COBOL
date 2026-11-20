       IDENTIFICATION DIVISION.
       PROGRAM-ID. PGTRAN.
      *================================================================*
      * OPERACION 03 : TRANSFERENCIA ENTRE CUENTAS                     *
      * COMISION : 1% DEL IMPORTE, MINIMO 2.00, MAXIMO 50.00           *
      * RETORNA  : LS-RETCODE 00=OK 02=IMPORTE 03=FONDOS 04=REGLA      *
      *================================================================*
       ENVIRONMENT DIVISION.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-REGLAS.
           05  WS-MAX-TRANSF        PIC 9(11)V99 VALUE 5000000.00.
           05  WS-COM-MIN           PIC 9(7)V99  VALUE 2.00.
           05  WS-COM-MAX           PIC 9(7)V99  VALUE 50.00.
       01  WS-CALCULOS.
           05  WS-REQUERIDO         PIC 9(13)V99 VALUE ZERO.
       01  WS-EDICION.
           05  WS-SALDO-ED          PIC ZZZZZZZZZZZ9.99.
           05  WS-COM-ED            PIC ZZZZZ9.99.
       LINKAGE SECTION.
       COPY CBOPERAC.
       PROCEDURE DIVISION USING LS-OPERACION.
       MAIN-TRANSFERENCIA.
           MOVE "00" TO LS-RETCODE
           MOVE SPACES TO LS-MENSAJE
           MOVE ZERO TO LS-COMISION
           MOVE LS-SALDO-IN TO LS-SALDO-OUT
           MOVE LS-SALDO-DEST-IN TO LS-SALDO-DEST-OUT
      *    --- VALIDACIONES ---
           IF LS-CUENTA-ORIGEN = SPACES
              MOVE "04" TO LS-RETCODE
              MOVE "CUENTA ORIGEN REQUERIDA" TO LS-MENSAJE
              GOBACK
           END-IF
           IF LS-CUENTA-DESTINO = SPACES
              MOVE "04" TO LS-RETCODE
              MOVE "CUENTA DESTINO REQUERIDA" TO LS-MENSAJE
              GOBACK
           END-IF
           IF LS-CUENTA-ORIGEN = LS-CUENTA-DESTINO
              MOVE "04" TO LS-RETCODE
              MOVE "LAS CUENTAS DEBEN SER DISTINTAS" TO LS-MENSAJE
              GOBACK
           END-IF
           IF LS-IMPORTE = ZERO
              MOVE "02" TO LS-RETCODE
              MOVE "IMPORTE DEBE SER MAYOR QUE CERO" TO LS-MENSAJE
              GOBACK
           END-IF
           IF LS-IMPORTE > WS-MAX-TRANSF
              MOVE "04" TO LS-RETCODE
              MOVE "IMPORTE EXCEDE EL MAXIMO" TO LS-MENSAJE
              GOBACK
           END-IF
      *    --- COMISION 1% ENTRE MINIMO Y MAXIMO ---
           COMPUTE LS-COMISION ROUNDED =
                   LS-IMPORTE * 0.01
           IF LS-COMISION < WS-COM-MIN
              MOVE WS-COM-MIN TO LS-COMISION
           END-IF
           IF LS-COMISION > WS-COM-MAX
              MOVE WS-COM-MAX TO LS-COMISION
           END-IF
      *    --- FONDOS (IMPORTE + COMISION) ---
           COMPUTE WS-REQUERIDO =
                   LS-IMPORTE + LS-COMISION
           IF WS-REQUERIDO > LS-SALDO-IN
              MOVE "03" TO LS-RETCODE
              MOVE "FONDOS INSUFICIENTES (INCLUYE COMISION)"
                   TO LS-MENSAJE
              GOBACK
           END-IF
      *    --- OPERACION ---
           COMPUTE LS-SALDO-OUT ROUNDED =
                   LS-SALDO-IN - WS-REQUERIDO
           COMPUTE LS-SALDO-DEST-OUT ROUNDED =
                   LS-SALDO-DEST-IN + LS-IMPORTE
           MOVE LS-COMISION TO WS-COM-ED
           MOVE LS-SALDO-OUT TO WS-SALDO-ED
           STRING "TRANSFERENCIA APROBADA. COMISION: "
                  DELIMITED BY SIZE
                  WS-COM-ED DELIMITED BY SIZE
                  " SALDO ORIGEN: "
                  DELIMITED BY SIZE
                  WS-SALDO-ED DELIMITED BY SIZE
                  INTO LS-MENSAJE
           END-STRING
           GOBACK.
       END PROGRAM PGTRAN.
