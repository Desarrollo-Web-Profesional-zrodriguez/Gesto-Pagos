package com.proyecto.servicios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import com.proyecto.servicios.service.Impl.PersonasServiceImpl;

@ExtendWith(MockitoExtension.class)
class PersonasServiceImplTest {

    @Mock
    private PersonasRepository personasRepository;

    @InjectMocks
    private PersonasServiceImpl personasService;

    @Test
    @DisplayName("creaPersona: guarda la persona correctamente y retorna PersonaResponse con código 1")
    void creaPersona_Exitoso() {
        PersonasRequest request = new PersonasRequest();
        request.setNombre("Carlos");
        request.setApellidoP("Gomez");
        request.setApellidoMaterno("Perez");

        when(personasRepository.save(any(Personas.class))).thenAnswer(invocation -> {
            Personas p = invocation.getArgument(0);
            p.setId(10);
            return p;
        });

        PersonaResponse response = personasService.creaPersona(request);

        assertNotNull(response);
        assertEquals(1, response.getCodigo());
        assertEquals("Exito", response.getMensaje());
        assertEquals("Carlos", response.getNombre());
        assertEquals("Gomez", response.getApellidoP());
        assertEquals("Perez", response.getApellidoMaterno());

        ArgumentCaptor<Personas> captor = ArgumentCaptor.forClass(Personas.class);
        verify(personasRepository).save(captor.capture());
        Personas saved = captor.getValue();
        assertEquals("Carlos", saved.getNombre());
        assertEquals("Gomez", saved.getApellidoP());
        assertEquals("Perez", saved.getApellidoMaterno());
    }

    @Test
    @DisplayName("eliminaPersona: cuando la persona existe, la elimina y retorna código 0")
    void eliminaPersona_Exitoso() {
        EliminaPersonaRequest request = new EliminaPersonaRequest();
        request.setNombre("Carlos");

        Personas personaExistente = new Personas();
        personaExistente.setId(1);
        personaExistente.setNombre("Carlos");
        personaExistente.setApellidoP("Gomez");
        personaExistente.setApellidoMaterno("Perez");

        when(personasRepository.findByNombre("Carlos")).thenReturn(Optional.of(personaExistente));

        GenericResponse response = personasService.eliminaPersona(request);

        assertNotNull(response);
        assertEquals(0, response.getCodigo());
        assertEquals("La persona ha sido eliminada correctamente", response.getMensaje());
        verify(personasRepository).delete(personaExistente);
    }

    @Test
    @DisplayName("eliminaPersona: cuando la persona no existe, retorna código 1 con mensaje correspondiente")
    void eliminaPersona_NoExiste() {
        EliminaPersonaRequest request = new EliminaPersonaRequest();
        request.setNombre("Desconocido");

        when(personasRepository.findByNombre("Desconocido")).thenReturn(Optional.empty());

        GenericResponse response = personasService.eliminaPersona(request);

        assertNotNull(response);
        assertEquals(1, response.getCodigo());
        assertEquals("La persona no existe ", response.getMensaje());
        verify(personasRepository, never()).delete(any(Personas.class));
    }

    @Test
    @DisplayName("actualizaPersona: cuando la persona existe, actualiza apellidos y retorna código 0")
    void actualizaPersona_Exitoso() {
        PersonasRequest request = new PersonasRequest();
        request.setNombre("Carlos");
        request.setApellidoP("Lopez");
        request.setApellidoMaterno("Hernandez");

        Personas personaExistente = new Personas();
        personaExistente.setId(1);
        personaExistente.setNombre("Carlos");
        personaExistente.setApellidoP("Gomez");
        personaExistente.setApellidoMaterno("Perez");

        when(personasRepository.findByNombre("Carlos")).thenReturn(Optional.of(personaExistente));
        when(personasRepository.save(any(Personas.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GenericResponse response = personasService.actualizaPersona(request);

        assertNotNull(response);
        assertEquals(0, response.getCodigo());
        assertEquals("la persona ha sido actualizada correctamente", response.getMensaje());

        verify(personasRepository).save(personaExistente);
        assertEquals("Lopez", personaExistente.getApellidoP());
        assertEquals("Hernandez", personaExistente.getApellidoMaterno());
    }

    @Test
    @DisplayName("actualizaPersona: cuando la persona no existe, retorna código 1")
    void actualizaPersona_NoExiste() {
        PersonasRequest request = new PersonasRequest();
        request.setNombre("Desconocido");
        request.setApellidoP("Lopez");
        request.setApellidoMaterno("Hernandez");

        when(personasRepository.findByNombre("Desconocido")).thenReturn(Optional.empty());

        GenericResponse response = personasService.actualizaPersona(request);

        assertNotNull(response);
        assertEquals(1, response.getCodigo());
        assertEquals("La persona no existe ", response.getMensaje());
        verify(personasRepository, never()).save(any(Personas.class));
    }
}
