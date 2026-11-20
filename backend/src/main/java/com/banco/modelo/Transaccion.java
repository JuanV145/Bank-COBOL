package com.banco.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * MODELO - Registro histórico de cada operación enviada al DLL COBOL.
 * Se guarda siempre: APROBADA, RECHAZADA o TIMEOUT.
 */
@Entity
@Table(name = "transacciones")
@Getter
@Setter
@NoArgsConstructor
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 12)
    private String referencia;

    @Column(nullable = false, length = 15)
    private String tipo;

    @Column(nullable = false, length = 12)
    private String cuentaOrigen;

    @Column(length = 12)
    private String cuentaDestino;

    @Column(nullable = false)
    private long importeCents;

    @Column(nullable = false)
    private long comisionCents;

    @Column(nullable = false, length = 10)
    private String resultado;

    @Column(nullable = false, length = 2)
    private String retCode;

    @Column(nullable = false, length = 120)
    private String mensaje;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    public BigDecimal importe() {
        return BigDecimal.valueOf(importeCents, 2);
    }

    public BigDecimal comision() {
        return BigDecimal.valueOf(comisionCents, 2);
    }

    public void asignarImporte(BigDecimal valor) {
        this.importeCents = valor.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2).longValueExact();
    }

    public void asignarComision(BigDecimal valor) {
        this.comisionCents = valor.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2).longValueExact();
    }
}
