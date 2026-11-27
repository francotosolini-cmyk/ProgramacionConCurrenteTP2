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
    private final ControlTemporal control;
    // Hilos que anunciaron (con el mutex tomado) que van a esperar en cada transición.
    // Se usa en lugar de Colas.hilosEsperando() para no perder despertares: un hilo
    // cuenta como esperando aunque todavía no haya llegado al acquire() del semáforo.
    private final int[] esperando;

    public Monitor(RedPetri redPetri, Politica politica, Logger logger) {
        this(redPetri, politica, logger, null);
    }

    public Monitor(RedPetri redPetri, Politica politica, Logger logger, long[] alphaMs) {
        this.redPetri = redPetri;
        this.politica = politica;
        this.mutex = new Mutex();
        this.logger = logger;
        this.colas = new Colas(redPetri.getTransicionesCount());
        this.esperando = new int[redPetri.getTransicionesCount()];
        this.control = new ControlTemporal(alphaMs, redPetri.getSensitisedTransitions());
    }

    @Override
    public boolean fireTransition(int t){
        mutex.adquirir();

        while (true){

            // Acá el hilo siempre tiene el mutex (lo adquirió al entrar o lo recibió por
            // paso de testigo). Si la red ya terminó, lo libera y sale sin despertar a nadie:
            // despertarTodos() ya lo llamó el hilo que hizo el último disparo.
            if (redPetri.isFinished()) {
                mutex.liberar();
                return false;
            }

            // Pregunta si está sensibilizada para poder evaluar la ventana temporal.
            // Si otro hilo ya duerme la ventana de t, se la trata como no sensibilizada.
            boolean sensibilizada = redPetri.isSensitised(t)
                    && !control.hayAlguienEsperandoVentana(t);

            if (sensibilizada) {
                ControlTemporal.ResultadoVentana ventana = control.evaluarVentanaTemporal(t);

                if (ventana == ControlTemporal.ResultadoVentana.ANTES_DE_VENTANA) {
                    long espera = control.tiempoEspera(t);
                    control.setEsperandoVentana(t);
                    signalNext();                                   // cede el testigo o libera el mutex
                    boolean interrumpido = control.dormir(espera);  // duerme fuera del mutex
                    mutex.adquirir();
                    control.limpiarEsperandoVentana(t);             // el flag se baja después del acquire
                    if (interrumpido) {
                        Thread.currentThread().interrupt();
                        mutex.liberar();
                        return false;
                    }
                    continue;                                       // vuelve al inicio y reevalúa
                }

                // Transición inmediata o dentro de la ventana: dispara
                boolean[] antes = redPetri.getSensitisedTransitions();
                if (!redPetri.fire(t)) {
                    throw new IllegalStateException("fire(" + t + ") falló estando sensibilizada");
                }
                control.actualizarTimestamps(antes, redPetri.getSensitisedTransitions(), t);

                // Registra el disparo en el log
                logger.logTransition(t);

                // Este disparo terminó la red: se llama a despertarTodos()
                if (redPetri.isFinished()) {
                    colas.despertarTodos();
                    mutex.liberar();
                    return true;
                }

                // Despierta a un hilo (paso de testigo) o libera el mutex si no hay ninguno
                signalNext();
                return true;
            }

            // No sensibilizada (o ventana ocupada por otro hilo):
            esperando[t]++;
            mutex.liberar();
            colas.esperar(t);

            // Despertado por despertarTodos(): no tiene el mutex, sale sin liberarlo
            // ni volver a despertar a nadie.
            if (redPetri.isFinished()) {
                return false;
            }
            // Despertado por despertar(): recibió el mutex (paso de testigo), reintenta (loop)
        }
    }

    /**
     * Calcula los hilos despertables: transiciones sensibilizadas que además
     * tienen hilos anotados como esperando. Debe invocarse con el mutex tomado.
     *
     * @return boolean[] donde la posición t indica si hay un hilo despertable en t.
     */
    private boolean[] getWakeableThreads() {
        boolean[] sensitised = redPetri.getSensitisedTransitions();
        boolean[] wakeableThreads = new boolean[redPetri.getTransicionesCount()];

        for (int i = 0; i < sensitised.length; i++) {
            wakeableThreads[i] = esperando[i] > 0
                    && sensitised[i]
                    && !control.hayAlguienEsperandoVentana(i);
        }
        return wakeableThreads;
    }

    /**
     * Si hay hilos para despertar, la política elige uno y se le pasa el mutex
     * (paso de testigo). Si no hay ninguno, libera el mutex.
     * Debe invocarse con el mutex tomado.
     */
    private void signalNext() {
        boolean[] wakeableThreads = getWakeableThreads();
        boolean areThreadsWakeable = false;

        // indica si hay por lo menos un hilo para despertar
        for (boolean wakeable : wakeableThreads) {
            if (wakeable) {
                areThreadsWakeable = true;
                break;
            }
        }

        if (areThreadsWakeable) {
            //Si hay uno o más hilos para despertar, la politica elije a quien despertar
            int selectedThread = politica.elegir(wakeableThreads);
            esperando[selectedThread]--;
            colas.despertar(selectedThread);
        } else {
            //Si no hay hilos para despertar, libre el mutex
            mutex.liberar();
        }
    }
}
