package ar.edu.unc.fcefyn.pcp.tpfinal.politica;

import ar.edu.unc.fcefyn.pcp.tpfinal.red.ConfigPagos;

public class PriorityPolicy implements Politica {
    private final RandomPolicy politicaAleatoria = new RandomPolicy();

    @Override
    public int elegir(boolean[] candidatas) {
        if (candidatas[ConfigPagos.T4]) {
            return ConfigPagos.T4;
        }

        return politicaAleatoria.elegir(candidatas);
    }
}
