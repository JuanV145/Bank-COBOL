package com.banco.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * MODELO - Cuenta bancaria. El saldo se guarda en centavos para conservar
 * exactitud al intercambiarlo con COBOL.
 */
@Entity
@Table(name = "cuentas")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Cuenta {

    @Id
    @Column(length = 12)
    private String numero;

    @Column(nullable = false, length = 40)
    private String titular;

    @Column(nullable = false, length = 1)
    private String tipo;

    @Column(nullable = false)
    private long saldoCents;

    @Column(nullable = false, length = 10)
    private String estado;

    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    public BigDecimal saldo() {
        return BigDecimal.valueOf(saldoCents, 2);
    }

    public void asignarSaldo(BigDecimal valor) {
        this.saldoCents = valor.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2).longValueExact();
    }
}
