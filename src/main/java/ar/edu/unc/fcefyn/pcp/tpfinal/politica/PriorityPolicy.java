package ar.edu.unc.fcefyn.pcp.tpfinal.politica;

import ar.edu.unc.fcefyn.pcp.tpfinal.red.ConfigPagos;
import java.util.concurrent.ThreadLocalRandom;

public class PriorityPolicy implements Politica {
    private final RandomPolicy politicaAleatoria = new RandomPolicy();
    private final double probPrioridad;

    public PriorityPolicy(double probPrioridad) {
        if (!(probPrioridad >= 0 && probPrioridad <= 1)) {
            throw new IllegalArgumentException("La probabilidad de prioridad debe estar entre 0 y 1");
        }
        this.probPrioridad = probPrioridad;
    }

    /**
     * Elige entre las candidatas (transiciones sensibilizadas con hilos esperando).
     * Si T4 (alto riesgo) es candidata, con probabilidad probPrioridad se la elige;
     * si no, se elige al azar entre todas las candidatas.
     */
    @Override
    public int elegir(boolean[] candidatas) {
        if (candidatas[ConfigPagos.T4]
                && ThreadLocalRandom.current().nextDouble() < probPrioridad) {
            return ConfigPagos.T4;
        }
        return politicaAleatoria.elegir(candidatas);
    }

    public double getProbPrioridad() {
        return probPrioridad;
    }
}
