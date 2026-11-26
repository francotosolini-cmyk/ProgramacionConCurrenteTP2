package ar.edu.unc.fcefyn.pcp.tpfinal.politica;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomPolicyTest {
    private final RandomPolicy randomPolicy = new RandomPolicy();

    @Test
    void eligeElIndiceDeLaUnicaCandidata() {
        int indiceElegido = randomPolicy.elegir(new boolean[]{false, false, true});

        assertEquals(2, indiceElegido);
    }

    @Test
    void devuelveMenosUnoSiNoHayCandidatas() {
        assertEquals(-1, randomPolicy.elegir(new boolean[]{false, false, false}));
        assertEquals(-1, randomPolicy.elegir(new boolean[]{}));
    }

    @Test
    void variasCandidatas() {
        for (int i = 0; i < 50; i++) {
            int indiceElegido = randomPolicy.elegir(new boolean[]{false, true, false, true});
            assertTrue(indiceElegido == 1 || indiceElegido == 3,
                    "Se eligió un índice inválido: " + indiceElegido);
        }
    }
}
