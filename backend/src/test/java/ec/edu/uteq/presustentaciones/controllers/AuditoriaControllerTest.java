package ec.edu.uteq.presustentaciones.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre RF-60 (consulta de auditoría) contra AuditoriaController real, con Postgres real
 * (mismo criterio que {@code PreSustentacionesApplicationTests}/
 * {@code TemaPropuestoRepositoryIntegrationTest}: {@code @AutoConfigureTestDatabase(replace = NONE)}
 * para no sustituir el datasource por H2). Hace falta el contexto completo -- no un
 * {@code @WebMvcTest} con {@code permisoService} mockeado como en {@code ActaControllerTest} --
 * porque el permiso AUDITORIA_VER se resuelve con una consulta real (rol_id -> rol_permisos,
 * ver {@code PermisoRepository.usuarioTienePermiso}) y porque la garantía de "nunca se guarda
 * el password" la da el trigger real {@code fn_auditoria_generica} (V15__auditoria.sql), no
 * código Java que se pueda mockear: solo se puede comprobar contra la fila que ese trigger
 * escribió de verdad.
 *
 * <p>Usa los usuarios semilla (ver {@code PreSustentacionesApplication.initDemoData}):
 * admin@uteq.edu.ec (ADMIN, único rol con AUDITORIA_VER -- V15 lo asigna solo a rol_id=1) y
 * demo@uteq.edu.ec (COORDINADOR).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.test.database.replace=NONE")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@AutoConfigureMockMvc
@Transactional
class AuditoriaControllerTest {

    private static final String ADMIN = "admin@uteq.edu.ec";
    private static final String COORDINADOR = "demo@uteq.edu.ec";
    private static final String DOCENTE = "docente@uteq.edu.ec";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sinAutenticarDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/auditoria/paginado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = DOCENTE)
    void sinPermisoAuditoriaVerDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/auditoria/paginado"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = COORDINADOR)
    void coordinadorTambienRecibe403PorqueElPermisoEsExclusivoDeAdmin() throws Exception {
        // Asimetria deliberada (SRS §6.4): a diferencia de otros permisos administrativos que
        // ADMIN y COORDINADOR comparten, AUDITORIA_VER solo se asigna al rol ADMIN en V15.
        mockMvc.perform(get("/api/v1/auditoria/paginado"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN)
    void adminConPermisoObtieneLaPaginaFiltradaPorTabla() throws Exception {
        // Garantiza al menos una fila real con tabla=usuarios antes de filtrar.
        modificarTelefonoDeUnUsuarioDemo();

        MvcResult resultado = mockMvc.perform(get("/api/v1/auditoria/paginado")
                        .param("tabla", "usuarios")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andReturn();

        // Un ResponseBodyAdvice global envuelve toda respuesta en ResponseWrapper (success/data/...),
        // aunque el controller devuelva el Page directamente: el body real es data.content, no content.
        JsonNode contenido = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("data").get("content");
        assertNotNull(contenido);
        assertTrue(contenido.size() > 0, "Debe existir al menos un evento de auditoria para 'usuarios'");
        for (JsonNode fila : contenido) {
            assertEquals("usuarios", fila.get("tabla").asText());
        }
    }

    @Test
    @WithMockUser(username = ADMIN)
    void ningunaEntradaDeAuditoriaDeUsuariosExponeElPassword() throws Exception {
        Long registroId = modificarTelefonoDeUnUsuarioDemo();

        MvcResult resultado = mockMvc.perform(get("/api/v1/auditoria/paginado")
                        .param("tabla", "usuarios")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andReturn();

        String cuerpo = resultado.getResponse().getContentAsString();
        // Comprobacion sobre el resultado completo tal cual lo recibe el cliente: ni siquiera
        // como texto plano dentro del JSON (datos_anteriores/datos_nuevos viajan como string).
        assertFalse(cuerpo.contains("\"password\""),
                "La respuesta de auditoria no debe exponer el campo password en ninguna entrada: " + cuerpo);

        JsonNode contenido = objectMapper.readTree(cuerpo).get("data").get("content");
        JsonNode filaDelCambio = buscarPorRegistroId(contenido, registroId);
        assertNotNull(filaDelCambio, "Debe aparecer el evento generado por el cambio de telefono de esta prueba");

        JsonNode datosNuevos = objectMapper.readTree(filaDelCambio.get("datosNuevos").asText());
        assertFalse(datosNuevos.has("password"), "fn_auditoria_generica debe haber quitado 'password' de datos_nuevos");
        // Prueba de que sí se guardó el resto de la fila (no es un objeto vacío por otra razón).
        assertTrue(datosNuevos.has("telefono"));

        if (filaDelCambio.hasNonNull("datosAnteriores")) {
            JsonNode datosAnteriores = objectMapper.readTree(filaDelCambio.get("datosAnteriores").asText());
            assertFalse(datosAnteriores.has("password"), "fn_auditoria_generica debe haber quitado 'password' de datos_anteriores");
        }
    }

    @Test
    @WithMockUser(username = ADMIN)
    void tablasAuditadasDevuelveElCatalogoFijoDeTablas() throws Exception {
        mockMvc.perform(get("/api/v1/auditoria/tablas"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("usuarios")));
    }

    private JsonNode buscarPorRegistroId(JsonNode contenido, Long registroId) {
        for (JsonNode fila : contenido) {
            if (fila.get("registroId").asLong() == registroId) {
                return fila;
            }
        }
        return null;
    }

    /**
     * Dispara trg_auditoria_usuarios con un UPDATE real e inofensivo (cambia el teléfono del
     * usuario docente demo). Corre dentro de la transacción de la prueba (@Transactional hace
     * rollback al terminar), así que no deja rastro en los datos semilla.
     */
    private Long modificarTelefonoDeUnUsuarioDemo() {
        Usuario usuario = usuarioRepository.findByEmail(DOCENTE).orElseThrow();
        usuario.setTelefono("099" + (System.nanoTime() % 10_000_000L));
        usuarioRepository.saveAndFlush(usuario);
        return usuario.getId();
    }
}
