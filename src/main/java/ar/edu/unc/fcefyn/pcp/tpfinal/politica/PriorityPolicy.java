package ar.edu.unc.fcefyn.pcp.tpfinal.politica;
import ar.edu.unc.fcefyn.pcp.tpfinal.red.ConfigPagos;
import java.util.concurrent.ThreadLocalRandom;

public class PriorityPolicy implements Politica {
    private final RandomPolicy politicaAleatoria = new RandomPolicy();
    private final double probPrioridad;
    
    public PriorityPolicy(double probPrioridad) {
        if(!(probPrioridad >= 0 && probPrioridad <= 1)) {
            throw new IllegalArgumentException("La probabilidad de prioridad debe estar entre 0 y 1");
        }
        this.probPrioridad = probPrioridad;
    }

    @Override
    public boolean[] permitidas(boolean[] sensibilizadas) {
        boolean[] permitidas = sensibilizadas.clone();
        if (sensibilizadas[ConfigPagos.T4]
        && ThreadLocalRandom.current().nextDouble() < probPrioridad) {
            permitidas[ConfigPagos.T1] = false;
            permitidas[ConfigPagos.T6] = false;
        }
        return permitidas;
    }
    @Override
    public int elegir(boolean[] candidatas) {
        return politicaAleatoria.elegir(candidatas);
    }
}
