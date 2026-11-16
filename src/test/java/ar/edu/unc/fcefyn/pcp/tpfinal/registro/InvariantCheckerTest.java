package ar.edu.unc.fcefyn.pcp.tpfinal.registro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class InvariantCheckerTest {

    @Test
    @DisplayName("Debe validar y contar un invariante individual de Tarjeta (T0 T1 T2 T3 T9)")
    void testInvarianteTarjetaSimple() {
        String seq = "T0 T1 T2 T3 T9";
        InvariantChecker.ResultadoConteo res = InvariantChecker.reducirSecuencia(seq);

        assertTrue(res.valido);
        assertEquals(1, res.tarjeta);
        assertEquals(0, res.altoRiesgo);
        assertEquals(0, res.transferencia);
        assertEquals("", res.resto);
    }

    @Test
    @DisplayName("Debe validar y contar un invariante individual de Alto Riesgo (T0 T4 T5 T9)")
    void testInvarianteAltoRiesgoSimple() {
        String seq = "T0 T4 T5 T9";
        InvariantChecker.ResultadoConteo res = InvariantChecker.reducirSecuencia(seq);

        assertTrue(res.valido);
        assertEquals(0, res.tarjeta);
        assertEquals(1, res.altoRiesgo);
        assertEquals(0, res.transferencia);
        assertEquals("", res.resto);
    }

    @Test
    @DisplayName("Debe validar y contar un invariante individual de Transferencia (T0 T6 T7 T8 T9)")
    void testInvarianteTransferenciaSimple() {
        String seq = "T0 T6 T7 T8 T9";
        InvariantChecker.ResultadoConteo res = InvariantChecker.reducirSecuencia(seq);

        assertTrue(res.valido);
        assertEquals(0, res.tarjeta);
        assertEquals(0, res.altoRiesgo);
        assertEquals(1, res.transferencia);
        assertEquals("", res.resto);
    }

    @Test
    @DisplayName("Debe reducir correctamente secuencias fuertemente intercaladas")
    void testInvariantesIntercalados() {
        // Intercalación: Tarjeta y Transferencia en concurrencia
        String seq = "T0 T0 T1 T6 T2 T7 T3 T8 T9 T9";
        InvariantChecker.ResultadoConteo res = InvariantChecker.reducirSecuencia(seq);

        assertTrue(res.valido, "La secuencia intercalada debe reducirse completamente");
        assertEquals(1, res.tarjeta);
        assertEquals(0, res.altoRiesgo);
        assertEquals(1, res.transferencia);
        assertEquals("", res.resto);
    }

    @Test
    @DisplayName("Debe fallar si una secuencia queda incompleta (ej. falta T9)")
    void testSecuenciaIncompletaFalla() {
        String seq = "T0 T1 T2 T3"; // Falta T9
        InvariantChecker.ResultadoConteo res = InvariantChecker.reducirSecuencia(seq);

        assertFalse(res.valido);
        assertEquals("T0 T1 T2 T3", res.resto);
        assertEquals(0, res.getTotalInvariantes());
    }

    @Test
    @DisplayName("Debe fallar y aislar la transición si queda una transición huérfana")
    void testTransicionHuerfanaFalla() {
        String seq = "T0 T4 T5 T9 T2"; // T2 queda huérfana
        InvariantChecker.ResultadoConteo res = InvariantChecker.reducirSecuencia(seq);

        assertFalse(res.valido);
        assertEquals(1, res.altoRiesgo);
        assertEquals("T2", res.resto);
    }

    @Test
    @DisplayName("Debe procesar desde archivo de log 200 invariantes completados (Req. 8 y 13)")
    void testProcesarArchivoLog200Invariantes(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("ejecucion-200.log");
        Logger logger = new Logger(logFile.toString());

        // Generar 100 ciclos de tarjeta, 50 de alto riesgo y 50 de transferencia
        for (int i = 0; i < 100; i++) {
            logger.logTransition(0);
            logger.logTransition(1);
            logger.logTransition(2);
            logger.logTransition(3);
            logger.logTransition(9);
        }
        for (int i = 0; i < 50; i++) {
            logger.logTransition(0);
            logger.logTransition(4);
            logger.logTransition(5);
            logger.logTransition(9);
        }
        for (int i = 0; i < 50; i++) {
            logger.logTransition(0);
            logger.logTransition(6);
            logger.logTransition(7);
            logger.logTransition(8);
            logger.logTransition(9);
        }
        logger.close();

        // Verificar con InvariantChecker
        assertTrue(InvariantChecker.verifyTransitionInvariants(logFile.toString()));

        int[] conteos = InvariantChecker.countByInvariant(logFile.toString());
        assertEquals(100, conteos[0], "Tarjeta");
        assertEquals(50, conteos[1], "Alto Riesgo");
        assertEquals(50, conteos[2], "Transferencia");
        assertEquals(200, conteos[0] + conteos[1] + conteos[2]);
    }
}
