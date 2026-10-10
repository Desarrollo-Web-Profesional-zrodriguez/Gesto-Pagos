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
import com.proyecto.servicios.model.cliente.ClienteIdentificadorRequest;
import com.proyecto.servicios.model.cliente.ClientePatchRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.service.cliente.ClienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

    // 2. Consultar clientes (todos o por identificador en query param)
    @GetMapping
    @Operation(summary = "Consultar clientes", description = "Retorna todos los clientes. Si se envia el parametro 'identificador' (RFC, CURP, correo o cuenta), retorna el cliente especifico.")
    public ResponseEntity<?> obtenerClientes(
            @Parameter(description = "RFC, CURP, correo electronico o numero de cuenta (opcional)", example = "PELJ920520HDFRRN09") 
            @RequestParam(value = "identificador", required = false) String identificador) {
        if (identificador != null && !identificador.trim().isEmpty()) {
            return ResponseEntity.ok(clienteService.obtenerPorIdentificador(identificador));
        }
        return ResponseEntity.ok(clienteService.obtenerTodos());
    }

    // 3. Consultar clientes activos
    @GetMapping("/activos")
    @Operation(summary = "Consultar clientes activos")
    public ResponseEntity<List<ClienteResponse>> obtenerClientesActivos() {
        return ResponseEntity.ok(clienteService.obtenerClientesActivos());
    }

    // 4. Búsqueda avanzada por POST
    @PostMapping("/buscar")
    @Operation(summary = "Busqueda avanzada de clientes por coincidencias (POST)", description = "Busca clientes por coincidencias parciales enviando JSON con CURP, RFC, correo electronico (ej. 'juan') o numero de cuenta. Retorna todas las coincidencias encontradas sin exponer el ID.")
    public ResponseEntity<List<ClienteResponse>> busquedaAvanzada(
            @RequestBody ClienteBusquedaAvanzadaRequest request) {
        return ResponseEntity.ok(clienteService.busquedaAvanzada(request));
    }

    // 5. Consultar clientes registrados en un rango de fechas
    @GetMapping("/fechas")
    @Operation(summary = "Consultar clientes registrados en un rango de fechas")
    public ResponseEntity<List<ClienteResponse>> obtenerPorRangoFechas(
            @Parameter(description = "Fecha inicial YYYY-MM-DD", example = "2026-01-01") 
            @RequestParam("inicio") @NotNull(message = "La fecha de inicio es requerida") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @Parameter(description = "Fecha final YYYY-MM-DD", example = "2026-12-31") 
            @RequestParam("fin") @NotNull(message = "La fecha final es requerida") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(clienteService.obtenerPorRangoFechas(inicio, fin));
    }

    // 6. Actualizacion Completa (PUT) por cuerpo del request
    @PutMapping
    @Operation(summary = "Actualizar informacion completa del cliente", description = "Busca por identificador, RFC, CURP o correo en el cuerpo del request. No permite modificar CURP, RFC ni numero de cuenta")
    public ResponseEntity<ClienteResponse> actualizarCompleto(@Valid @RequestBody ClienteRegistroRequest request) {
        return ResponseEntity.ok(clienteService.actualizarCompleto(request));
    }

    // 7. Actualizacion Parcial (PATCH) por cuerpo del request
    @PatchMapping
    @Operation(summary = "Actualizacion parcial de datos (PATCH)", description = "Busca por identificador (o correo) en el cuerpo del request. Permite modificar solo los campos enviados, protegiendo CURP y RFC")
    public ResponseEntity<ClienteResponse> actualizarParcial(@Valid @RequestBody ClientePatchRequest request) {
        return ResponseEntity.ok(clienteService.actualizarParcial(request));
    }

    // 8. Baja Logica (DELETE) por cuerpo del request o parametro
    @DeleteMapping
    @Operation(summary = "Baja logica del cliente", description = "Busca por RFC, CURP, correo o numero de cuenta enviado en el cuerpo de la peticion (o parametro). Desactiva al cliente y suspende automaticamente sus cuentas asociadas")
    public ResponseEntity<Void> desactivarCliente(
            @RequestBody(required = false) ClienteIdentificadorRequest body,
            @Parameter(description = "RFC, CURP, correo o cuenta (si no se envia en body)", example = "PELJ920520HDFRRN09") 
            @RequestParam(value = "identificador", required = false) String paramIdentificador) {
        String idFinal = (body != null && body.getIdentificador() != null && !body.getIdentificador().trim().isEmpty())
                ? body.getIdentificador().trim() : paramIdentificador;
        if (idFinal == null || idFinal.trim().isEmpty()) {
            throw new ReglaNegocioException("Debe proporcionar el identificador del cliente en el request");
        }
        clienteService.desactivarClientePorIdentificador(idFinal.trim());
        return ResponseEntity.noContent().build();
    }

    // 9. Reactivacion de Cliente (PATCH) por cuerpo del request o parametro
    @PatchMapping("/reactivar")
    @Operation(summary = "Reactivar cliente desactivado (PATCH)", description = "Busca por RFC, CURP, correo o numero de cuenta enviado en el cuerpo de la peticion (o parametro) y reactiva al cliente y sus cuentas")
    public ResponseEntity<ClienteResponse> reactivarCliente(
            @RequestBody(required = false) ClienteIdentificadorRequest body,
            @Parameter(description = "RFC, CURP, correo o cuenta (si no se envia en body)", example = "PELJ920520HDFRRN09") 
            @RequestParam(value = "identificador", required = false) String paramIdentificador) {
        String idFinal = (body != null && body.getIdentificador() != null && !body.getIdentificador().trim().isEmpty())
                ? body.getIdentificador().trim() : paramIdentificador;
        if (idFinal == null || idFinal.trim().isEmpty()) {
            throw new ReglaNegocioException("Debe proporcionar el identificador del cliente en el request");
        }
        return ResponseEntity.ok(clienteService.reactivarClientePorIdentificador(idFinal.trim()));
    }
}
