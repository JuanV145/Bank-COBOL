       IDENTIFICATION DIVISION.
       PROGRAM-ID. PGSALDO.
      *================================================================*
      * OPERACION 04 : CONSULTA DE SALDO                               *
      * RECIBE  : LS-OPERACION (COPY CBOPERAC) mapeado por JNA         *
      * RETORNA : LS-RETCODE 00=OK 04=REGLA DE NEGOCIO                 *
      *================================================================*
       ENVIRONMENT DIVISION.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-EDICION.
           05  WS-SALDO-ED          PIC ZZZZZZZZZZZ9.99.
       LINKAGE SECTION.
       COPY CBOPERAC.
       PROCEDURE DIVISION USING LS-OPERACION.
       MAIN-CONSULTA.
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
      *    --- OPERACION ---
           MOVE LS-SALDO-OUT TO WS-SALDO-ED
           STRING "CONSULTA OK. SALDO DISPONIBLE: "
                  DELIMITED BY SIZE
                  WS-SALDO-ED DELIMITED BY SIZE
                  INTO LS-MENSAJE
           END-STRING
           GOBACK.
       END PROGRAM PGSALDO.
