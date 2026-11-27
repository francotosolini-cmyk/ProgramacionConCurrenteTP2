package ar.edu.unc.fcefyn.pcp.tpfinal.politica;

import ar.edu.unc.fcefyn.pcp.tpfinal.red.ConfigPagos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriorityPolicyTest {



    @Test
    void cuandoT4EsCandidataSiempreLaElige() {
        PriorityPolicy policy = new PriorityPolicy(ConfigPagos.T4);
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
        PriorityPolicy policy = new PriorityPolicy(ConfigPagos.T4);
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
    void cuandoNoHayCandidatasDevuelveMenosUno() {
        PriorityPolicy policy = new PriorityPolicy(ConfigPagos.T4);
        boolean[] candidatas = new boolean[ConfigPagos.TRANSICIONES_COUNT];

        assertEquals(-1, policy.elegir(candidatas));
    }

    @Test
    void indiceNegativoEsInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> new PriorityPolicy(-1));
    }

    @Test
    void priorizaLaTransicionRecibidaPorConstructor() {
        PriorityPolicy policy = new PriorityPolicy(ConfigPagos.T1);

        boolean[] candidatas = new boolean[ConfigPagos.TRANSICIONES_COUNT];
        candidatas[ConfigPagos.T1] = true;
        candidatas[ConfigPagos.T4] = true;

        assertEquals(ConfigPagos.T1, policy.elegir(candidatas));
    }

}
