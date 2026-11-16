package ar.edu.unc.fcefyn.pcp.tpfinal.politica;
import ar.edu.unc.fcefyn.pcp.tpfinal.red.ConfigPagos;

public class PriorityPolicy implements Politica {
    private final RandomPolicy politicaAleatoria = new RandomPolicy();
    @Override
    public boolean[] permitidas(boolean[] sensibilizadas) {
        boolean[] permitidas = sensibilizadas.clone();
        if (sensibilizadas[ConfigPagos.T4]) {
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
