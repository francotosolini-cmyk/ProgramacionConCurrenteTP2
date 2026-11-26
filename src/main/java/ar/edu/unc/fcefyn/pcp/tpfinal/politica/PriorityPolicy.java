package ar.edu.unc.fcefyn.pcp.tpfinal.politica;

/**
 * Política de prioridad estricta.
 * Si la transición prioritaria es candidata, se la elige siempre. Si no lo es,
 * se elige al azar, con distribución uniforme, entre las candidatas restantes.
 * La transición prioritaria se recibe por constructor: la política no conoce la red.
 */
public class PriorityPolicy implements Politica {
    private final RandomPolicy politicaAleatoria = new RandomPolicy();
    private final int transicionPrioritaria;

    public PriorityPolicy(int transicionPrioritaria) {
        if (transicionPrioritaria < 0) {
            throw new IllegalArgumentException("La transición prioritaria debe ser un índice no negativo");
        }
        this.transicionPrioritaria = transicionPrioritaria;
    }

    @Override
    public int elegir(boolean[] candidatas) {
        if (transicionPrioritaria < candidatas.length && candidatas[transicionPrioritaria]) {
            return transicionPrioritaria;
        }
        // La prioritaria no es candidata: sorteo uniforme entre las demás
        return politicaAleatoria.elegir(candidatas);
    }

    public int getTransicionPrioritaria() {
        return transicionPrioritaria;
    }
}
