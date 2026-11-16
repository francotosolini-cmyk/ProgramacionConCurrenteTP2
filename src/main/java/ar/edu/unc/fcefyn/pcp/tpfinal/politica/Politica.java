package ar.edu.unc.fcefyn.pcp.tpfinal.politica;

public interface Politica {

    public boolean[] permitidas(boolean[] sensibilizadas);

    public int elejir(boolean[] candidatas);
}
