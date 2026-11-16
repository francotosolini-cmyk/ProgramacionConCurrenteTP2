package ar.edu.unc.fcefyn.pcp.tpfinal.politica;

import org.junit.jupiter.api.Test; //Para poder usar la anotacion @Test

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals; 
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomPolicyTest {
    private final RandomPolicy randomPolicy = new RandomPolicy();
    @Test
    void eligeElIndiceDeLaUnicaCandidata() { //Comprueba que el método elegir devuelve el índice correcto cuando hay una única candidata
        int indiceElegido = randomPolicy.elegir(new boolean[]{false, false, true});

        assertEquals(2, indiceElegido);
    }
    @Test
    void devuelveMenosUnoSiNoHayCandidatas() { //Comprueba que el método elegir devuelve -1 cuando no hay candidatas
        int indiceElegido = randomPolicy.elegir(new boolean[]{false, false, false});

        assertEquals(-1, indiceElegido);
    }
    @Test
    void variasCandidatas() { //Comprueba que el método elegir devuelve un índice válido cuando hay varias candidatas. Se ejecuta varias veces para aumentar la probabilidad de cubrir todos los casos posibles
        for(int i = 0; i<5; i++){
            int indiceElegido = randomPolicy.elegir(new boolean[]{false, true, false, true});
            assertTrue(indiceElegido == 1 || indiceElegido == 3,
                    "Se eligió un índice inválido: " + indiceElegido);
        }
    }
    @Test
    void OpcionesPermitidas() { //Comprueba que el método permitidas devuelve un array con los mismos valores que el array de entrada, pero no es el mismo objeto
        boolean sensibilizadas[] = new boolean[]{false, true, false, true};
        boolean[] permitidas = randomPolicy.permitidas(sensibilizadas);
        assertArrayEquals(sensibilizadas,permitidas);
        assertNotSame(sensibilizadas, permitidas);
    }
}
