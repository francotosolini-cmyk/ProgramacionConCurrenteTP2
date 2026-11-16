package ar.edu.unc.fcefyn.pcp.tpfinal.politica;

import org.junit.jupiter.api.Test;

import ar.edu.unc.fcefyn.pcp.tpfinal.red.ConfigPagos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PriorityPolicyTest {
    private final PriorityPolicy priorityPolicy = new PriorityPolicy(1.0);
    private final PriorityPolicy priorityPolicy_2 = new PriorityPolicy(0.0);
    @Test
    public void T4Habilitada(){
        boolean[] sensibilizadas = new boolean[10];
        sensibilizadas[ConfigPagos.T0] = true;
        sensibilizadas[ConfigPagos.T1] = true;
        sensibilizadas[ConfigPagos.T4] = true;
        sensibilizadas[ConfigPagos.T6] = true;
        sensibilizadas[ConfigPagos.T9] = true;
        boolean[] permitidas = priorityPolicy.permitidas(sensibilizadas);
        boolean[] esperado = new boolean[10];
        esperado[ConfigPagos.T0] = true;
        esperado[ConfigPagos.T4] = true;
        esperado[ConfigPagos.T9] = true;
        assertArrayEquals(esperado, permitidas);
        assertTrue(sensibilizadas[ConfigPagos.T1]);
        assertTrue(sensibilizadas[ConfigPagos.T6]);
    }
    @Test
    public void T4Deshabilitada(){
        boolean[] sensibilizadas = new boolean[10];
        sensibilizadas[ConfigPagos.T1] = true;
        sensibilizadas[ConfigPagos.T6] = true;
        boolean[] permitidas = priorityPolicy.permitidas(sensibilizadas);
        boolean[] esperado = new boolean[10];
        esperado[ConfigPagos.T1] = true;
        esperado[ConfigPagos.T6] = true;
        assertArrayEquals(esperado, permitidas);
    }
    @Test
    public void T4HabilitadaPeroProbabilidadCero(){
        boolean[] sensibilizadas = new boolean[10];
        sensibilizadas[ConfigPagos.T1] = true;
        sensibilizadas[ConfigPagos.T4] = true;
        sensibilizadas[ConfigPagos.T6] = true;
        boolean[] permitidas = priorityPolicy_2.permitidas(sensibilizadas);
        boolean[] esperado = new boolean[10];
        esperado[ConfigPagos.T1] = true;
        esperado[ConfigPagos.T4] = true;
        esperado[ConfigPagos.T6] = true;
        assertArrayEquals(esperado, permitidas);
    }
}
