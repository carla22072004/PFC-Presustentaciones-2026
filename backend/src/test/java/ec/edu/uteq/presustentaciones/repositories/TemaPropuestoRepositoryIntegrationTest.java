package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.TemaPropuesto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración real (mismo criterio que {@code PreSustentacionesApplicationTests}:
 * {@code @SpringBootTest} + {@code @AutoConfigureTestDatabase(replace = NONE)} para usar el
 * Postgres real, no un repositorio mockeado) para {@link TemaPropuestoRepository#buscarConFiltros}.
 *
 * <p>Hallazgo real (verificación manual contra el backend en Docker, 2026-09-04): con
 * {@code nivelDificultad == null}, la consulta fallaba con
 * "ERROR: function lower(bytea) does not exist" -- Postgres no podía inferir el tipo del
 * parámetro dentro de {@code LOWER(?)} cuando el valor bindeado era null. Ninguna prueba lo
 * detectaba porque {@code TemaServiceImplTest} mockea {@code TemaPropuestoRepository} por
 * completo: {@code when(repo.buscarConFiltros(...)).thenReturn(...)} nunca ejecuta el JPQL
 * real contra Postgres, así que un bug de traducción JPQL-a-SQL como este es invisible a nivel
 * de mock. Esta clase reproduce el JPQL real contra la base real -- exactamente el escenario
 * que hizo fallar la petición HTTP -- y se corrigió con {@code CAST(:nivel AS string)} en el
 * repositorio (ver su comentario).
 */
@SpringBootTest(properties = "spring.test.database.replace=NONE")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class TemaPropuestoRepositoryIntegrationTest {

    @Autowired
    private TemaPropuestoRepository temaPropuestoRepository;

    @Test
    void buscarConFiltros_sinNingunFiltro_noLanzaYDevuelveLista() {
        List<TemaPropuesto> resultado = assertDoesNotThrow(
                () -> temaPropuestoRepository.buscarConFiltros(null, null, null, null));
        assertNotNull(resultado);
    }

    @Test
    void buscarConFiltros_soloNivelDificultadNull_noLanza() {
        // El caso exacto que rompía: algún filtro presente, nivel ausente.
        assertDoesNotThrow(() -> temaPropuestoRepository.buscarConFiltros(1, null, null, null));
    }

    @Test
    void buscarConFiltros_soloCarreraId_noLanza() {
        assertDoesNotThrow(() -> temaPropuestoRepository.buscarConFiltros(1, null, null, null));
    }

    @Test
    void buscarConFiltros_soloLineaInvestigacionId_noLanza() {
        assertDoesNotThrow(() -> temaPropuestoRepository.buscarConFiltros(null, 1, null, null));
    }

    @Test
    void buscarConFiltros_soloNivelDificultad_noLanzaYFiltraCorrectamente() {
        List<TemaPropuesto> resultado = assertDoesNotThrow(
                () -> temaPropuestoRepository.buscarConFiltros(null, null, null, "BASICO"));
        assertNotNull(resultado);
        for (TemaPropuesto t : resultado) {
            assertTrue("BASICO".equalsIgnoreCase(t.getNivelDificultad()));
        }
    }

    @Test
    void buscarConFiltros_nivelDificultadEnMinusculas_esCaseInsensitive() {
        // LOWER(...) = LOWER(CAST(...)) debe seguir siendo insensible a mayúsculas/minúsculas.
        List<TemaPropuesto> mayus = temaPropuestoRepository.buscarConFiltros(null, null, null, "BASICO");
        List<TemaPropuesto> minus = temaPropuestoRepository.buscarConFiltros(null, null, null, "basico");
        assertNotNull(minus);
        assertTrue(mayus.size() == minus.size());
    }

    @Test
    void buscarConFiltros_combinacionDeFiltros_noLanza() {
        assertDoesNotThrow(() -> temaPropuestoRepository.buscarConFiltros(1, 1, null, "INTERMEDIO"));
    }

    @Test
    void buscarConFiltros_areaIdSinNivel_noLanza() {
        assertDoesNotThrow(() -> temaPropuestoRepository.buscarConFiltros(null, null, 1, null));
    }
}
