package ar.edu.unc.fcefyn.pcp.tpfinal.politica;

/**
 * Política de prioridad estricta.
 * La transición prioritaria se recibe por constructor.
 */
public class PriorityPolicy implements Politica {
    private final RandomPolicy politicaAleatoria = new RandomPolicy();
    private final int transicionPrioritaria;

    public PriorityPolicy(int transicionPrioritaria) {
        if (transicionPrioritaria < 0) {
            throw new IllegalArgumentException(
                "La transición prioritaria debe ser un índice no negativo"
            );
        }
        this.transicionPrioritaria = transicionPrioritaria;
    }

    @Override
    public int elegir(boolean[] candidatas) {
        if (transicionPrioritaria < candidatas.length
                && candidatas[transicionPrioritaria]) {
            return transicionPrioritaria;
        }

        return politicaAleatoria.elegir(candidatas);
    }

    public int getTransicionPrioritaria() {
        return transicionPrioritaria;
    }
}