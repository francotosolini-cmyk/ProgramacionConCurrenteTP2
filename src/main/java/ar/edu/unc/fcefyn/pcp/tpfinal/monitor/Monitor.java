package ar.edu.unc.fcefyn.pcp.tpfinal.monitor;

import ar.edu.unc.fcefyn.pcp.tpfinal.politica.Politica;
import ar.edu.unc.fcefyn.pcp.tpfinal.red.RedPetri;
import ar.edu.unc.fcefyn.pcp.tpfinal.registro.Logger;

public class Monitor implements MonitorInterface {
    private final RedPetri redPetri;
    private final Politica politica;
    private final Mutex mutex;
    private final Colas colas;
    private final Logger logger;
    // Hilos que anunciaron (con el mutex tomado) que van a esperar en cada transición.
    // Se usa en lugar de Colas.hilosEsperando() para no perder despertares: un hilo
    // cuenta como esperando aunque todavía no haya llegado al acquire() del semáforo.
    private final int[] esperando;

    public Monitor(RedPetri redPetri, Politica politica, Logger logger) {
        this.redPetri = redPetri;
        this.politica = politica;
        this.mutex = new Mutex();
        this.logger = logger;
        this.colas = new Colas(redPetri.getTransicionesCount());
        this.esperando = new int[redPetri.getTransicionesCount()];
    }

    @Override
    public boolean fireTransition(int t){
        mutex.adquirir();

        while (true) {
            if (redPetri.isFinished()) {
                mutex.liberar();
                return false;
            }

            // Se consulta la política también al entrar (no solo al despertar), para que
            // un hilo que llega con su transición sensibilizada no se saltee la prioridad.
            // Contrato: permitidas() devuelve un subconjunto de las sensibilizadas y nunca
            // todo en false si hay alguna sensibilizada.
            boolean[] permitidas = politica.permitidas(redPetri.getSensitisedTransitions());
            if (permitidas[t]) {
                redPetri.fire(t);
                logger.logTransition(t);
                if (redPetri.isFinished()) {
                    liberarTodos();       // despierta a todos para que terminen
                    mutex.liberar();
                } else {
                    signalNext();         // pasa el testigo o libera el mutex
                }
                return true;
            }

            // No puede disparar: se anota como esperando y, antes de dormir, pasa el
            // testigo a otro candidato (o libera el mutex si no hay ninguno).
            esperando[t]++;
            signalNext();
            colas.esperar(t);

            // Despertado por liberarTodos(): no tiene el mutex, sale sin liberarlo
            if (redPetri.isFinished()) {
                return false;
            }
            // Despertado por signalNext(): recibió el mutex (paso de testigo), reintenta
        }
    }

    /**
     * Calcula las transiciones candidatas a despertar: aquellas sensibilizadas
     * y permitidas por la política que además tienen hilos esperando en su cola.
     * Debe invocarse con el mutex tomado.
     *
     * @return boolean[] donde la posición t indica si la transición t es candidata.
     */
    private boolean[] getAwaitingSensitisedTransitions(){
        boolean[] permitidas = politica.permitidas(redPetri.getSensitisedTransitions());
        boolean[] candidatas = new boolean[permitidas.length];
        for (int t = 0; t < candidatas.length; t++) {
            candidatas[t] = permitidas[t] && t < esperando.length && esperando[t] > 0;
        }
        return candidatas;
    }

    private void signalNext(){
        boolean[] candidatas = getAwaitingSensitisedTransitions();
        for (boolean c : candidatas) {
            if (c) {
                int elegida = politica.elegir(candidatas);
                esperando[elegida]--;
                colas.despertar(elegida); // el mutex pasa al despertado (paso de testigo)
                return;
            }
        }
        mutex.liberar();
    }

    private void liberarTodos(){
        colas.despertarTodos();

    }
}
