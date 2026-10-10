package com.proyecto.servicios.controller.cliente;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.servicios.model.cliente.ClientePatchRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.service.cliente.ClienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

@Slf4j
@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Clientes", description = "Operaciones de Onboarding, consulta, modificacion y baja logica de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    // 1. Registro de Cliente (Onboarding)
    @PostMapping
    @Operation(summary = "Registrar nuevo cliente", description = "Crea el cliente, valida mayoria de edad, genera cuenta bancaria automatica con saldo inicial y usuario con password cifrado/biometria")
    public ResponseEntity<ClienteResponse> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        log.info("[REST] Solicitud para registrar cliente con CURP: {}", request.getCurp());
        ClienteResponse response = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Consultar todos los clientes
    @GetMapping
    @Operation(summary = "Consultar todos los clientes")
    public ResponseEntity<List<ClienteResponse>> obtenerTodos() {
        return ResponseEntity.ok(clienteService.obtenerTodos());
    }

    // 3. Consultar cliente por ID
    @GetMapping("/{id}")
    @Operation(summary = "Consultar cliente por ID")
    public ResponseEntity<ClienteResponse> obtenerPorId(
            @Parameter(description = "ID del cliente (mayor a cero)", example = "1") 
            @PathVariable @Positive(message = "El ID del cliente debe ser un numero mayor a cero") Long id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    // 4. Consultar clientes activos
    @GetMapping("/activos")
    @Operation(summary = "Consultar clientes activos")
    public ResponseEntity<List<ClienteResponse>> obtenerClientesActivos() {
        return ResponseEntity.ok(clienteService.obtenerClientesActivos());
    }

    // 5. Consultas especificas (CURP, RFC, Correo, Cuenta)
    @GetMapping("/buscar/curp/{curp}")
    @Operation(summary = "Buscar cliente por CURP")
    public ResponseEntity<ClienteResponse> obtenerPorCurp(
            @Parameter(description = "CURP oficial del cliente (exactamente 18 caracteres)", example = "PELJ920520HDFRRN09") 
            @PathVariable 
            @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$", message = "La CURP debe tener exactamente 18 caracteres y cumplir el formato oficial (ejemplo: PELJ920520HDFRRN09)") 
            String curp) {
        return ResponseEntity.ok(clienteService.obtenerPorCurp(curp));
    }

    @GetMapping("/buscar/rfc/{rfc}")
    @Operation(summary = "Buscar cliente por RFC")
    public ResponseEntity<ClienteResponse> obtenerPorRfc(
            @Parameter(description = "RFC oficial del cliente (12 o 13 caracteres)", example = "PELJ9205201A0") 
            @PathVariable 
            @Pattern(regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-V1-9][A-Z1-9][0-9A]$", message = "El RFC debe tener 12 o 13 caracteres y cumplir el formato oficial (ejemplo: PELJ9205201A0)") 
            String rfc) {
        return ResponseEntity.ok(clienteService.obtenerPorRfc(rfc));
    }

    @GetMapping("/buscar/correo/{correo}")
    @Operation(summary = "Buscar cliente por correo electronico")
    public ResponseEntity<ClienteResponse> obtenerPorCorreo(
            @Parameter(description = "Correo electronico del cliente", example = "juan.perez@example.com") 
            @PathVariable 
            @Pattern(regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "El formato del correo electronico no es valido (ejemplo: usuario@dominio.com)") 
            @Size(max = 100, message = "El correo electronico no debe exceder 100 caracteres") 
            String correo) {
        return ResponseEntity.ok(clienteService.obtenerPorCorreo(correo));
    }

    @GetMapping("/buscar/cuenta/{numeroCuenta}")
    @Operation(summary = "Buscar cliente por numero de cuenta")
    public ResponseEntity<ClienteResponse> obtenerPorNumeroCuenta(
            @Parameter(description = "Numero de cuenta bancaria (10 digitos)", example = "1000000001") 
            @PathVariable 
            @Pattern(regexp = "^\\d{10}$", message = "El numero de cuenta debe tener exactamente 10 digitos numericos") 
            String numeroCuenta) {
        return ResponseEntity.ok(clienteService.obtenerPorNumeroCuenta(numeroCuenta));
    }

    // 6. Consultar clientes registrados en un rango de fechas
    @GetMapping("/fechas")
    @Operation(summary = "Consultar clientes registrados en un rango de fechas")
    public ResponseEntity<List<ClienteResponse>> obtenerPorRangoFechas(
            @Parameter(description = "Fecha inicial YYYY-MM-DD", example = "2026-01-01") 
            @RequestParam("inicio") @NotNull(message = "La fecha de inicio es requerida") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @Parameter(description = "Fecha final YYYY-MM-DD", example = "2026-12-31") 
            @RequestParam("fin") @NotNull(message = "La fecha final es requerida") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(clienteService.obtenerPorRangoFechas(inicio, fin));
    }

    // 7. Actualizacion Completa (PUT)
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar informacion completa del cliente", description = "No permite modificar CURP, RFC ni numero de cuenta")
    public ResponseEntity<ClienteResponse> actualizarCompleto(
            @Parameter(description = "ID del cliente (mayor a cero)", example = "1") 
            @PathVariable @Positive(message = "El ID del cliente debe ser un numero mayor a cero") Long id, 
            @Valid @RequestBody ClienteRegistroRequest request) {
        return ResponseEntity.ok(clienteService.actualizarCompleto(id, request));
    }

    // 8. Actualizacion Parcial (PATCH)
    @PatchMapping("/{id}")
    @Operation(summary = "Actualizacion parcial de datos (PATCH)", description = "Permite modificar solo los campos enviados, protegiendo CURP y RFC")
    public ResponseEntity<ClienteResponse> actualizarParcial(
            @Parameter(description = "ID del cliente (mayor a cero)", example = "1") 
            @PathVariable @Positive(message = "El ID del cliente debe ser un numero mayor a cero") Long id, 
            @Valid @RequestBody ClientePatchRequest request) {
        return ResponseEntity.ok(clienteService.actualizarParcial(id, request));
    }

    // 9. Baja Logica (DELETE)
    @DeleteMapping("/{id}")
    @Operation(summary = "Baja logica del cliente", description = "Desactiva al cliente y suspende automaticamente sus cuentas asociadas")
    public ResponseEntity<Void> desactivarCliente(
            @Parameter(description = "ID del cliente (mayor a cero)", example = "1") 
            @PathVariable @Positive(message = "El ID del cliente debe ser un numero mayor a cero") Long id) {
        clienteService.desactivarCliente(id);
        return ResponseEntity.noContent().build();
    }

    // 10. Reactivacion de Cliente (PATCH)
    @PatchMapping("/{id}/reactivar")
    @Operation(summary = "Reactivar cliente desactivado (PATCH)")
    public ResponseEntity<ClienteResponse> reactivarCliente(
            @Parameter(description = "ID del cliente (mayor a cero)", example = "1") 
            @PathVariable @Positive(message = "El ID del cliente debe ser un numero mayor a cero") Long id) {
        return ResponseEntity.ok(clienteService.reactivarCliente(id));
    }
}
