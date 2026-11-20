       IDENTIFICATION DIVISION.
       PROGRAM-ID. PGCIERRE.
      *================================================================*
      * OPERACION 05 : CIERRE DE CUENTA                                *
      * REGLA    : LA CUENTA DEBE QUEDAR EN SALDO CERO                 *
      * RETORNA  : LS-RETCODE 00=OK 04=REGLA DE NEGOCIO                *
      *================================================================*
       ENVIRONMENT DIVISION.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       LINKAGE SECTION.
       COPY CBOPERAC.
       PROCEDURE DIVISION USING LS-OPERACION.
       MAIN-CIERRE.
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
           IF LS-SALDO-IN NOT = ZERO
              MOVE "04" TO LS-RETCODE
              MOVE "CUENTA CON SALDO PENDIENTE" TO LS-MENSAJE
              GOBACK
           END-IF
      *    --- OPERACION ---
           MOVE "CIERRE APROBADO. CUENTA QUEDA INACTIVA"
                TO LS-MENSAJE
           GOBACK.
       END PROGRAM PGCIERRE.
