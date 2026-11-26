package ar.edu.unc.fcefyn.pcp.tpfinal.politica;

import org.junit.jupiter.api.Test;

import ar.edu.unc.fcefyn.pcp.tpfinal.red.ConfigPagos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PriorityPolicyTest {
    private final PriorityPolicy priorityPolicy = new PriorityPolicy(ConfigPagos.T4);

    @Test
    public void prioritariaCandidataSeEligeSiempre() {
        boolean[] candidatas = new boolean[10];
        candidatas[ConfigPagos.T0] = true;
        candidatas[ConfigPagos.T1] = true;
        candidatas[ConfigPagos.T4] = true;
        candidatas[ConfigPagos.T6] = true;
        candidatas[ConfigPagos.T9] = true;
        for (int i = 0; i < 10_000; i++) {
            assertEquals(ConfigPagos.T4, priorityPolicy.elegir(candidatas));
        }
    }

    @Test
    public void prioritariaNoCandidataEligeEntreLasRestantes() {
        boolean[] candidatas = new boolean[10];
        candidatas[ConfigPagos.T1] = true;
        candidatas[ConfigPagos.T6] = true;
        int t1 = 0;
        int t6 = 0;
        for (int i = 0; i < 10_000; i++) {
            int elegida = priorityPolicy.elegir(candidatas);
            assertTrue(elegida == ConfigPagos.T1 || elegida == ConfigPagos.T6);
            if (elegida == ConfigPagos.T1) t1++; else t6++;
        }
        // Sorteo uniforme: ambas deben salir una cantidad comparable de veces
        assertTrue(t1 > 4_000 && t6 > 4_000, "t1=" + t1 + " t6=" + t6);
    }

    @Test
    public void sinCandidatasDevuelveMenosUno() {
        assertEquals(-1, priorityPolicy.elegir(new boolean[10]));
    }

    @Test
    public void indiceNegativoEsInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new PriorityPolicy(-1));
    }
}
