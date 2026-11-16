package ar.edu.unc.fcefyn.pcp.tpfinal.politica;

public interface Politica {
    boolean[] permitidas(boolean[] sensibilizadas);

    int elegir(boolean[] candidatas);
}
