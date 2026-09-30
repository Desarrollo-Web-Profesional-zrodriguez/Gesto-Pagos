package com.proyecto.servicios.service.cliente;

import java.time.LocalDate;
import java.util.List;

import com.proyecto.servicios.model.cliente.ClientePatchRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;

public interface ClienteService {

    // 1. Registro / Onboarding
    ClienteResponse registrarCliente(ClienteRegistroRequest request);

    // 2. Consultas
    List<ClienteResponse> obtenerTodos();

    ClienteResponse obtenerPorId(Long id);

    ClienteResponse obtenerPorCurp(String curp);

    ClienteResponse obtenerPorRfc(String rfc);

    ClienteResponse obtenerPorCorreo(String correo);

    ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta);

    List<ClienteResponse> obtenerClientesActivos();

    List<ClienteResponse> obtenerPorRangoFechas(LocalDate inicio, LocalDate fin);

    // 3. Actualizaciones
    ClienteResponse actualizarCompleto(Long id, ClienteRegistroRequest request);

    ClienteResponse actualizarParcial(Long id, ClientePatchRequest request);

    // 4. Baja Lógica
    void desactivarCliente(Long id);

    ClienteResponse reactivarCliente(Long id);
}
