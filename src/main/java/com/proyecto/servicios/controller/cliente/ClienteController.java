package com.proyecto.servicios.controller.cliente;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.servicios.model.cliente.ClienteBusquedaAvanzadaRequest;
import com.proyecto.servicios.model.cliente.ClienteIdentificadorRequest;
import com.proyecto.servicios.model.cliente.ClientePatchRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.service.cliente.ClienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Clientes", description = "Operaciones de registro, consulta combinada, actualizacion total y parcial (incluyendo activacion y baja logica) de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    // 1. Registro de Cliente
    @PostMapping
    @Operation(summary = "Registrar nuevo cliente", description = "Crea el cliente, valida mayoria de edad, genera cuenta bancaria automatica con saldo inicial y usuario con password cifrado/biometria. No expone ID del cliente.")
    public ResponseEntity<ClienteResponse> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        log.info("[REST] Solicitud para registrar cliente con CURP: {}", request.getCurp());
        ClienteResponse response = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Consulta y búsqueda de clientes (POST con Request DTO)
    @PostMapping("/buscar")
    @Operation(summary = "Consultar y buscar clientes (criterios combinables)", description = "Permite consultar todos los clientes o filtrar combinando criterios (identificador, CURP, RFC, correo, cuenta, estatus activo, rango de fechas) mediante Request DTO en el cuerpo de la peticion sin usar @RequestParam ni @PathVariable.")
    public ResponseEntity<List<ClienteResponse>> buscarClientes(
            @Valid @RequestBody(required = false) ClienteBusquedaAvanzadaRequest request) {
        return ResponseEntity.ok(clienteService.busquedaAvanzada(request));
    }

    // 2.1 Consulta individual de cliente por identificador (POST con Request DTO)
    @PostMapping("/consultar")
    @Operation(summary = "Consultar cliente individual por identificador", description = "Obtiene los datos de un cliente especifico por su CURP, RFC, correo electronico o numero de cuenta mediante Request DTO sin exponer ni utilizar IDs.")
    public ResponseEntity<ClienteResponse> consultarPorIdentificador(
            @Valid @RequestBody ClienteIdentificadorRequest request) {
        return ResponseEntity.ok(clienteService.obtenerPorIdentificador(request.getIdentificador()));
    }

    // 3. Actualizacion Completa (PUT) por cuerpo del request
    @PutMapping
    @Operation(summary = "Actualizar informacion completa del cliente", description = "Busca por identificador, RFC, CURP o correo en el cuerpo del request. No permite modificar CURP, RFC ni numero de cuenta.")
    public ResponseEntity<ClienteResponse> actualizarCompleto(@Valid @RequestBody ClienteRegistroRequest request) {
        return ResponseEntity.ok(clienteService.actualizarCompleto(request));
    }

    // 4. Actualizacion Parcial, activacion y baja logica (PATCH)
    @PatchMapping
    @Operation(summary = "Actualizacion parcial, activacion y baja logica (PATCH)", description = "Busca por identificador (o correo) en el cuerpo del request. Permite modificar solo los campos enviados y activar o desactivar al cliente enviando 'activo: true' o 'activo: false'.")
    public ResponseEntity<ClienteResponse> actualizarParcial(@Valid @RequestBody ClientePatchRequest request) {
        return ResponseEntity.ok(clienteService.actualizarParcial(request));
    }
}
