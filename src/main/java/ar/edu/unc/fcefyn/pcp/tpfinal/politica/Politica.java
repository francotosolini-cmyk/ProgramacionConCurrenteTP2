package ar.edu.unc.fcefyn.pcp.tpfinal.politica;

public interface Politica {

    /**
     * Elige a qué transición despertar entre las candidatas: transiciones sensibilizadas
     * que tienen hilos esperando. El Monitor la invoca una sola vez por disparo, desde
     * signalNext(), y solo cuando hay al menos una candidata.
     *
     * @param candidatas candidatas[t] indica si la transición t es candidata.
     * @return el índice de la transición elegida, o -1 si no hay ninguna candidata.
     */
    int elegir(boolean[] candidatas);
}