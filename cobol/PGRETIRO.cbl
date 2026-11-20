       IDENTIFICATION DIVISION.
       PROGRAM-ID. PGRETIRO.
      *================================================================*
      * OPERACION 02 : RETIRO DE CUENTA                                *
      * RECIBE  : LS-OPERACION (COPY CBOPERAC) mapeado por JNA         *
      * RETORNA : LS-RETCODE 00=OK 02=IMPORTE 03=FONDOS 04=REGLA       *
      *================================================================*
       ENVIRONMENT DIVISION.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-REGLAS.
           05  WS-MAX-RETIRO        PIC 9(11)V99 VALUE 2000000.00.
       01  WS-EDICION.
           05  WS-SALDO-ED          PIC ZZZZZZZZZZZ9.99.
       LINKAGE SECTION.
       COPY CBOPERAC.
       PROCEDURE DIVISION USING LS-OPERACION.
       MAIN-RETIRO.
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
           IF LS-IMPORTE = ZERO
              MOVE "02" TO LS-RETCODE
              MOVE "IMPORTE DEBE SER MAYOR QUE CERO" TO LS-MENSAJE
              GOBACK
           END-IF
           IF LS-IMPORTE > WS-MAX-RETIRO
              MOVE "04" TO LS-RETCODE
              MOVE "IMPORTE EXCEDE EL MAXIMO" TO LS-MENSAJE
              GOBACK
           END-IF
           IF LS-IMPORTE > LS-SALDO-IN
              MOVE "03" TO LS-RETCODE
              MOVE "FONDOS INSUFICIENTES PARA EL RETIRO" TO LS-MENSAJE
              GOBACK
           END-IF
      *    --- OPERACION ---
           COMPUTE LS-SALDO-OUT ROUNDED =
                   LS-SALDO-IN - LS-IMPORTE
           MOVE LS-SALDO-OUT TO WS-SALDO-ED
           STRING "RETIRO APROBADO. NUEVO SALDO: "
                  DELIMITED BY SIZE
                  WS-SALDO-ED DELIMITED BY SIZE
                  INTO LS-MENSAJE
           END-STRING
           GOBACK.
       END PROGRAM PGRETIRO.
