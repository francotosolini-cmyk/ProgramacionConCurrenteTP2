package ar.edu.unc.fcefyn.pcp.tpfinal.politica;

import ar.edu.unc.fcefyn.pcp.tpfinal.red.ConfigPagos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityPolicyTest {

    @Test
    void constructorValidaRangoDeProbabilidad() {
        assertEquals(0.0, new PriorityPolicy(0.0).getProbPrioridad());
        assertEquals(0.8, new PriorityPolicy(0.8).getProbPrioridad());
        assertEquals(1.0, new PriorityPolicy(1.0).getProbPrioridad());

        assertThrows(IllegalArgumentException.class, () -> new PriorityPolicy(-0.01));
        assertThrows(IllegalArgumentException.class, () -> new PriorityPolicy(1.01));
        assertThrows(IllegalArgumentException.class, () -> new PriorityPolicy(Double.NaN));
    }

    @Test
    void cuandoT4EsCandidataYProbabilidadEsUnoSiempreEligeT4() {
        PriorityPolicy policy = new PriorityPolicy(1.0);
        boolean[] candidatas = new boolean[ConfigPagos.TRANSICIONES_COUNT];
        candidatas[ConfigPagos.T0] = true;
        candidatas[ConfigPagos.T1] = true;
        candidatas[ConfigPagos.T4] = true;
        candidatas[ConfigPagos.T6] = true;
        candidatas[ConfigPagos.T9] = true;

        for (int i = 0; i < 100; i++) {
            assertEquals(ConfigPagos.T4, policy.elegir(candidatas));
        }
    }

    @Test
    void cuandoT4NoEsCandidataEligeEntreLasRestantes() {
        PriorityPolicy policy = new PriorityPolicy(1.0);
        boolean[] candidatas = new boolean[ConfigPagos.TRANSICIONES_COUNT];
        candidatas[ConfigPagos.T1] = true;
        candidatas[ConfigPagos.T6] = true;

        for (int i = 0; i < 50; i++) {
            int elegida = policy.elegir(candidatas);
            assertTrue(elegida == ConfigPagos.T1 || elegida == ConfigPagos.T6,
                    "Índice inesperado: " + elegida);
        }
    }

    @Test
    void cuandoProbabilidadEsCeroDelegaEnEleccionAleatoria() {
        PriorityPolicy policy = new PriorityPolicy(0.0);
        boolean[] candidatas = new boolean[ConfigPagos.TRANSICIONES_COUNT];
        candidatas[ConfigPagos.T1] = true;
        candidatas[ConfigPagos.T4] = true;
        candidatas[ConfigPagos.T6] = true;

        boolean eligioOtra = false;
        for (int i = 0; i < 200; i++) {
            int elegida = policy.elegir(candidatas);
            assertTrue(elegida == ConfigPagos.T1 || elegida == ConfigPagos.T4 || elegida == ConfigPagos.T6,
                    "Índice inesperado: " + elegida);
            if (elegida != ConfigPagos.T4) {
                eligioOtra = true;
            }
        }
        assertTrue(eligioOtra, "Con probabilidad 0 no debería elegir siempre T4");
    }

    @Test
    void cuandoNoHayCandidatasDevuelveMenosUno() {
        PriorityPolicy policy = new PriorityPolicy(1.0);
        boolean[] candidatas = new boolean[ConfigPagos.TRANSICIONES_COUNT];

        assertEquals(-1, policy.elegir(candidatas));
    }

    @Test
    void probabilidadIntermediaPriorizaT4SobreOtrasCandidatas() {
        PriorityPolicy policy = new PriorityPolicy(0.8);
        boolean[] candidatas = new boolean[ConfigPagos.TRANSICIONES_COUNT];
        candidatas[ConfigPagos.T1] = true;
        candidatas[ConfigPagos.T4] = true;
        candidatas[ConfigPagos.T6] = true;

        int vecesT4 = 0;
        int total = 1000;
        for (int i = 0; i < total; i++) {
            if (policy.elegir(candidatas) == ConfigPagos.T4) {
                vecesT4++;
            }
        }
        assertTrue(vecesT4 > 700, "Se esperaba alta prioridad para T4, pero fue elegida " + vecesT4 + "/" + total);
    }
}
