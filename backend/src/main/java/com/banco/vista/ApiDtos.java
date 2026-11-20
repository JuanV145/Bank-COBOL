package com.banco.vista;

import com.banco.modelo.CobolGateway;
import com.banco.modelo.Cuenta;
import com.banco.modelo.TipoOperacion;
import com.banco.modelo.Transaccion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VISTA - DTOs de entrada y salida de la API. Los records evitan el
 * boilerplate de getters/setters y conservan los nombres JSON del contrato.
 */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record CuentaRequest(
            @NotBlank(message = "El número de cuenta es obligatorio")
            @Pattern(regexp = "[A-Za-z0-9]{1,12}", message = "La cuenta debe ser alfanumérica de 1 a 12 caracteres")
            String numero,

            @NotBlank(message = "El titular es obligatorio")
            @Size(max = 40, message = "El titular debe tener máximo 40 caracteres")
            String titular,

            @NotBlank(message = "El tipo de cuenta es obligatorio")
            @Pattern(regexp = "[AaCc]", message = "El tipo debe ser A (ahorros) o C (corriente)")
            String tipo
    ) {
    }

    public record OperacionRequest(
            @NotBlank(message = "La cuenta origen es obligatoria")
            @Size(max = 12, message = "La cuenta origen debe tener máximo 12 caracteres")
            String cuentaOrigen,

            @Size(max = 12, message = "La cuenta destino debe tener máximo 12 caracteres")
            String cuentaDestino,

            @DecimalMin(value = "0.01", message = "El importe debe ser mayor que cero")
            @Digits(integer = 11, fraction = 2, message = "El importe no puede exceder 11 dígitos y 2 decimales")
            BigDecimal importe,

            Long timeoutMs,
            Long simularDemoraMs
    ) {
    }

    public record CuentaResponse(
            String numero,
            String titular,
            String tipo,
            long saldoCents,
            String estado,
            LocalDateTime fechaApertura
    ) {
        public static CuentaResponse de(Cuenta c) {
            return new CuentaResponse(c.getNumero(), c.getTitular(), c.getTipo(),
                    c.getSaldoCents(), c.getEstado(), c.getFechaApertura());
        }
    }

    public record TransaccionResponse(
            Long id,
            String referencia,
            String tipo,
            String cuentaOrigen,
            String cuentaDestino,
            long importeCents,
            long comisionCents,
            String resultado,
            String retCode,
            String mensaje,
            LocalDateTime fechaHora
    ) {
        public static TransaccionResponse de(Transaccion t) {
            return new TransaccionResponse(t.getId(), t.getReferencia(), t.getTipo(),
                    t.getCuentaOrigen(), t.getCuentaDestino(), t.getImporteCents(),
                    t.getComisionCents(), t.getResultado(), t.getRetCode(),
                    t.getMensaje(), t.getFechaHora());
        }
    }

    public record OperacionResponse(
            String referencia,
            String tipo,
            String resultado,
            String retCode,
            String mensaje,
            BigDecimal importe,
            BigDecimal comision,
            BigDecimal saldoOrigenFinal,
            BigDecimal saldoDestinoFinal,
            long duracionMs
    ) {
        public static OperacionResponse de(TipoOperacion tipo, Transaccion t, CobolGateway.Resultado r) {
            boolean aprobada = r != null && r.aprobada();
            return new OperacionResponse(
                    t.getReferencia(),
                    tipo.name(),
                    t.getResultado(),
                    t.getRetCode(),
                    t.getMensaje(),
                    t.importe(),
                    t.comision(),
                    aprobada ? r.saldoOrigen() : null,
                    aprobada && tipo == TipoOperacion.TRANSFERENCIA ? r.saldoDestino() : null,
                    r != null ? r.duracionMs() : 0
            );
        }
    }

    public record ApiError(String error, String mensaje, LocalDateTime fecha) {
        public static ApiError de(String mensaje) {
            return new ApiError("ERROR", mensaje, LocalDateTime.now());
        }

        public static ApiError de(String error, String mensaje) {
            return new ApiError(error, mensaje, LocalDateTime.now());
        }
    }
}
