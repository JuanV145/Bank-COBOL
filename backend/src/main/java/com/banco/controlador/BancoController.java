package com.banco.controlador;

import com.banco.modelo.BancoService;
import com.banco.modelo.Cuenta;
import com.banco.modelo.TipoOperacion;
import com.banco.vista.ApiDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CONTROLADOR - Expone la API REST y delega en el modelo (BancoService).
 * No contiene reglas de negocio: solo traduce HTTP <-> DTOs.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BancoController {

    private final BancoService service;

    @GetMapping("/cuentas")
    public List<ApiDtos.CuentaResponse> listarCuentas() {
        return service.listarCuentas().stream()
                .map(ApiDtos.CuentaResponse::de)
                .toList();
    }

    @PostMapping("/cuentas")
    public ResponseEntity<ApiDtos.CuentaResponse> crearCuenta(
            @Valid @RequestBody ApiDtos.CuentaRequest req) {
        Cuenta cuenta = service.crearCuenta(req.numero().trim(), req.titular(), req.tipo());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiDtos.CuentaResponse.de(cuenta));
    }

    @GetMapping("/cuentas/{numero}")
    public ApiDtos.CuentaResponse buscarCuenta(@PathVariable String numero) {
        return ApiDtos.CuentaResponse.de(service.buscarCuenta(numero));
    }

    @GetMapping("/cuentas/{numero}/transacciones")
    public List<ApiDtos.TransaccionResponse> historialCuenta(@PathVariable String numero) {
        return service.historialCuenta(numero).stream()
                .map(ApiDtos.TransaccionResponse::de)
                .toList();
    }

    @PostMapping("/operaciones/{tipo}")
    public ApiDtos.OperacionResponse operar(@PathVariable String tipo,
                                            @Valid @RequestBody ApiDtos.OperacionRequest req) {
        TipoOperacion operacion = TipoOperacion.desdeUrl(tipo);
        BancoService.Resultado r = service.operar(operacion, req.cuentaOrigen(),
                req.cuentaDestino(), req.importe(), req.timeoutMs(), req.simularDemoraMs());
        return ApiDtos.OperacionResponse.de(operacion, r.transaccion(), r.cobol());
    }

    @GetMapping("/transacciones")
    public List<ApiDtos.TransaccionResponse> historial(
            @RequestParam(defaultValue = "20") int limite) {
        int n = Math.min(200, Math.max(1, limite));
        return service.historial(n).stream()
                .map(ApiDtos.TransaccionResponse::de)
                .toList();
    }
}
