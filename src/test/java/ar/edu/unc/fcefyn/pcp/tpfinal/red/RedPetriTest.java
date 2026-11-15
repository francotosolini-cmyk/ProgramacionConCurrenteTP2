package ar.edu.unc.fcefyn.pcp.tpfinal.red;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedPetriTest {

    private ConfigPagos config;
    private RedPetri red;

    @BeforeEach
    void setUp() {
        config = new ConfigPagos();
        red = new RedPetri(config);
    }

    @Test
    @DisplayName("El marcado inicial y la sensibilización inicial deben ser correctos")
    void testMarcadoInicialYSensibilizacion() {
        int[] marcado = red.getMarcadoActual();

        assertEquals(3, marcado[ConfigPagos.P0], "P0 debe tener 3 tokens iniciales");
        assertEquals(0, marcado[ConfigPagos.P1]);
        assertEquals(0, marcado[ConfigPagos.P2]);
        assertEquals(0, marcado[ConfigPagos.P3]);
        assertEquals(0, marcado[ConfigPagos.P4]);
        assertEquals(0, marcado[ConfigPagos.P5]);
        assertEquals(0, marcado[ConfigPagos.P6]);
        assertEquals(1, marcado[ConfigPagos.P7], "P7 (Gateway) debe tener 1 token inicial");
        assertEquals(1, marcado[ConfigPagos.P8], "P8 (Motor antifraude) debe tener 1 token inicial");
        assertEquals(0, marcado[ConfigPagos.P9]);

        // En M0 solo T0 debe estar sensibilizada
        assertTrue(red.isSensitised(ConfigPagos.T0), "T0 debe estar sensibilizada en M0");
        for (int t = 1; t < red.getTransicionesCount(); t++) {
            assertFalse(red.isSensitised(t), "La transición T" + t + " no debe estar sensibilizada en M0");
        }
    }

    @Test
    @DisplayName("Debe procesar exitosamente el ciclo completo de pago con tarjeta (T0-T1-T2-T3-T9)")
    void testCicloTarjeta() {
        // T0: Admisión
        assertTrue(red.fire(ConfigPagos.T0));
        assertEquals(2, red.getMarcadoActual()[ConfigPagos.P0]);
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P1]);

        // T1: Ruteo tarjeta (consume P1, P7; produce P2)
        assertTrue(red.isSensitised(ConfigPagos.T1));
        assertTrue(red.fire(ConfigPagos.T1));
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P2]);
        assertEquals(0, red.getMarcadoActual()[ConfigPagos.P7]); // Gateway ocupado

        // T2: Autorización (consume P2; produce P3)
        assertTrue(red.isSensitised(ConfigPagos.T2));
        assertTrue(red.fire(ConfigPagos.T2));
        assertEquals(0, red.getMarcadoActual()[ConfigPagos.P2]);
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P3]);

        // T3: Captura (consume P3; produce P7, P9)
        assertTrue(red.isSensitised(ConfigPagos.T3));
        assertTrue(red.fire(ConfigPagos.T3));
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P7]); // Gateway liberado
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P9]); // Transacción en buffer salida

        // T9: Salida y confirmación (consume P9; produce P0)
        assertTrue(red.isSensitised(ConfigPagos.T9));
        assertTrue(red.fire(ConfigPagos.T9));
        assertEquals(3, red.getMarcadoActual()[ConfigPagos.P0]); // Token vuelve a P0
        assertEquals(0, red.getMarcadoActual()[ConfigPagos.P9]);
    }

    @Test
    @DisplayName("Debe procesar exitosamente el ciclo completo de pago de alto riesgo (T0-T4-T5-T9)")
    void testCicloAltoRiesgo() {
        // T0
        assertTrue(red.fire(ConfigPagos.T0));

        // T4: Ruteo alto riesgo (consume P1, P7 y P8; produce P4)
        assertTrue(red.isSensitised(ConfigPagos.T4));
        assertTrue(red.fire(ConfigPagos.T4));
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P4]);
        assertEquals(0, red.getMarcadoActual()[ConfigPagos.P7]); // Gateway ocupado
        assertEquals(0, red.getMarcadoActual()[ConfigPagos.P8]); // Motor ocupado

        // Ningún otro flujo puede entrar mientras P4 está ocupada
        assertFalse(red.isSensitised(ConfigPagos.T1));
        assertFalse(red.isSensitised(ConfigPagos.T6));

        // T5: Scoring finalizado (consume P4; produce P7, P8, P9)
        assertTrue(red.isSensitised(ConfigPagos.T5));
        assertTrue(red.fire(ConfigPagos.T5));
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P7]); // Gateway liberado
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P8]); // Motor liberado
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P9]);

        // T9
        assertTrue(red.fire(ConfigPagos.T9));
        assertEquals(3, red.getMarcadoActual()[ConfigPagos.P0]);
    }

    @Test
    @DisplayName("Debe procesar exitosamente el ciclo de transferencia bancaria (T0-T6-T7-T8-T9)")
    void testCicloTransferencia() {
        assertTrue(red.fire(ConfigPagos.T0));

        // T6: Ruteo transferencia (consume P1, P8; produce P5)
        assertTrue(red.isSensitised(ConfigPagos.T6));
        assertTrue(red.fire(ConfigPagos.T6));
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P5]);
        assertEquals(0, red.getMarcadoActual()[ConfigPagos.P8]); // Motor ocupado

        // T7: Validación completada (consume P5; produce P6)
        assertTrue(red.isSensitised(ConfigPagos.T7));
        assertTrue(red.fire(ConfigPagos.T7));
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P6]);

        // T8: Ejecución bancaria finalizada (consume P6; produce P8, P9)
        assertTrue(red.isSensitised(ConfigPagos.T8));
        assertTrue(red.fire(ConfigPagos.T8));
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P8]); // Motor liberado
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P9]);

        // T9
        assertTrue(red.fire(ConfigPagos.T9));
        assertEquals(3, red.getMarcadoActual()[ConfigPagos.P0]);
    }

    @Test
    @DisplayName("Tarjeta y Transferencia pueden coejecutarse concurrentemente sin colisionar")
    void testConcurrenciaTarjetaYTransferencia() {
        // Admitir dos transacciones a P1
        assertTrue(red.fire(ConfigPagos.T0));
        assertTrue(red.fire(ConfigPagos.T0));
        assertEquals(2, red.getMarcadoActual()[ConfigPagos.P1]);

        // T1 toma el gateway (P7) y pasa a P2
        assertTrue(red.fire(ConfigPagos.T1));
        // T6 toma el antifraude (P8) y pasa a P5
        assertTrue(red.fire(ConfigPagos.T6));

        // Ambas transacciones están en proceso a la vez
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P2]);
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P5]);
        assertEquals(0, red.getMarcadoActual()[ConfigPagos.P7]);
        assertEquals(0, red.getMarcadoActual()[ConfigPagos.P8]);

        // Alto riesgo (T4) NO puede ingresar porque ambos recursos están ocupados
        assertFalse(red.isSensitised(ConfigPagos.T4));

        // Avanzar ambas etapas en paralelo
        assertTrue(red.fire(ConfigPagos.T2)); // P2 -> P3
        assertTrue(red.fire(ConfigPagos.T7)); // P5 -> P6

        // Finalizar ambas etapas
        assertTrue(red.fire(ConfigPagos.T3)); // P3 -> P7, P9
        assertTrue(red.fire(ConfigPagos.T8)); // P6 -> P8, P9

        assertEquals(2, red.getMarcadoActual()[ConfigPagos.P9]);
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P7]);
        assertEquals(1, red.getMarcadoActual()[ConfigPagos.P8]);

        // Despachar ambas por T9
        assertTrue(red.fire(ConfigPagos.T9));
        assertTrue(red.fire(ConfigPagos.T9));
        assertEquals(3, red.getMarcadoActual()[ConfigPagos.P0]);
    }

    @Test
    @DisplayName("Disparo defensivo: fire() devuelve false si la transición no está sensibilizada")
    void testDisparoNoSensibilizado() {
        assertFalse(red.fire(ConfigPagos.T5), "No debe disparar T5 en marcado inicial");
        assertEquals(0, red.getFireCount(ConfigPagos.T5));
    }

    @Test
    @DisplayName("T0 debe dejar de sensibilizarse exactamente al alcanzar los 200 disparos")
    void testFrenoDeAdmisionEn200() {
        for (int i = 0; i < 200; i++) {
            assertTrue(red.isSensitised(ConfigPagos.T0), "T0 debe estar sensibilizada en la iteración " + i);
            assertTrue(red.fire(ConfigPagos.T0));
            // Despachar rápido por alto riesgo para recircular token a P0
            assertTrue(red.fire(ConfigPagos.T4));
            assertTrue(red.fire(ConfigPagos.T5));
            assertTrue(red.fire(ConfigPagos.T9));
        }

        assertEquals(200, red.getFireCount(ConfigPagos.T0));
        // En el disparo 201, T0 ya NO debe sensibilizarse aunque haya tokens en P0
        assertFalse(red.isSensitised(ConfigPagos.T0), "T0 no debe sensibilizarse tras 200 disparos");
        assertFalse(red.fire(ConfigPagos.T0));
        assertTrue(red.isFinished(), "La red debe considerarse finalizada al llegar T9 a 200");
    }
}
