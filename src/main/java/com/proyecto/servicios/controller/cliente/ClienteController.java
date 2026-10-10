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

import com.proyecto.servicios.model.cliente.ClienteBusquedaAvanzadaRequest;
import com.proyecto.servicios.model.cliente.ClientePatchRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.service.cliente.ClienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
    @Operation(summary = "Registrar nuevo cliente", description = "Crea el cliente, valida mayoria de edad, genera cuenta bancaria automatica con saldo inicial y usuario con password cifrado/biometria. No expone ID del cliente.")
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

    // 3. Consultar cliente por identificador (RFC, CURP, correo o número de cuenta)
    @GetMapping("/{identificador}")
    @Operation(summary = "Consultar cliente por identificador", description = "Busca el cliente mediante su RFC, CURP, correo electronico o numero de cuenta (tambien soporta ID numerico)")
    public ResponseEntity<ClienteResponse> obtenerPorIdentificador(
            @Parameter(description = "RFC, CURP, correo electronico o numero de cuenta", example = "PELJ920520HDFRRN09") 
            @PathVariable @NotBlank(message = "El identificador del cliente es requerido") String identificador) {
        return ResponseEntity.ok(clienteService.obtenerPorIdentificador(identificador));
    }

    // 4. Consultar clientes activos
    @GetMapping("/activos")
    @Operation(summary = "Consultar clientes activos")
    public ResponseEntity<List<ClienteResponse>> obtenerClientesActivos() {
        return ResponseEntity.ok(clienteService.obtenerClientesActivos());
    }

    // 5. Búsqueda avanzada por POST
    @PostMapping("/buscar")
    @Operation(summary = "Busqueda avanzada de clientes (POST)", description = "Busca uno o varios clientes enviando un JSON con cualquiera de los 4 criterios (CURP, RFC, correo electronico o numero de cuenta). Retorna la lista de clientes encontrados sin exponer su ID.")
    public ResponseEntity<List<ClienteResponse>> busquedaAvanzada(
            @RequestBody ClienteBusquedaAvanzadaRequest request) {
        return ResponseEntity.ok(clienteService.busquedaAvanzada(request));
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

    // 7. Actualizacion Completa (PUT) por RFC, CURP, correo o número de cuenta
    @PutMapping("/{identificador}")
    @Operation(summary = "Actualizar informacion completa del cliente", description = "Busca por RFC, CURP, correo o numero de cuenta. No permite modificar CURP, RFC ni numero de cuenta")
    public ResponseEntity<ClienteResponse> actualizarCompleto(
            @Parameter(description = "RFC, CURP, correo electronico o numero de cuenta", example = "PELJ920520HDFRRN09") 
            @PathVariable @NotBlank(message = "El identificador del cliente es requerido") String identificador, 
            @Valid @RequestBody ClienteRegistroRequest request) {
        return ResponseEntity.ok(clienteService.actualizarCompletoPorIdentificador(identificador, request));
    }

    // 8. Actualizacion Parcial (PATCH) por RFC, CURP, correo o número de cuenta
    @PatchMapping("/{identificador}")
    @Operation(summary = "Actualizacion parcial de datos (PATCH)", description = "Busca por RFC, CURP, correo o numero de cuenta. Permite modificar solo los campos enviados, protegiendo CURP y RFC")
    public ResponseEntity<ClienteResponse> actualizarParcial(
            @Parameter(description = "RFC, CURP, correo electronico o numero de cuenta", example = "PELJ920520HDFRRN09") 
            @PathVariable @NotBlank(message = "El identificador del cliente es requerido") String identificador, 
            @Valid @RequestBody ClientePatchRequest request) {
        return ResponseEntity.ok(clienteService.actualizarParcialPorIdentificador(identificador, request));
    }

    // 9. Baja Logica (DELETE) por RFC, CURP, correo o número de cuenta
    @DeleteMapping("/{identificador}")
    @Operation(summary = "Baja logica del cliente", description = "Busca por RFC, CURP, correo o numero de cuenta. Desactiva al cliente y suspende automaticamente sus cuentas asociadas")
    public ResponseEntity<Void> desactivarCliente(
            @Parameter(description = "RFC, CURP, correo electronico o numero de cuenta", example = "PELJ920520HDFRRN09") 
            @PathVariable @NotBlank(message = "El identificador del cliente es requerido") String identificador) {
        clienteService.desactivarClientePorIdentificador(identificador);
        return ResponseEntity.noContent().build();
    }

    // 10. Reactivacion de Cliente (PATCH) por RFC, CURP, correo o número de cuenta
    @PatchMapping("/{identificador}/reactivar")
    @Operation(summary = "Reactivar cliente desactivado (PATCH)", description = "Busca por RFC, CURP, correo o numero de cuenta y reactiva al cliente y sus cuentas")
    public ResponseEntity<ClienteResponse> reactivarCliente(
            @Parameter(description = "RFC, CURP, correo electronico o numero de cuenta", example = "PELJ920520HDFRRN09") 
            @PathVariable @NotBlank(message = "El identificador del cliente es requerido") String identificador) {
        return ResponseEntity.ok(clienteService.reactivarClientePorIdentificador(identificador));
    }
}
