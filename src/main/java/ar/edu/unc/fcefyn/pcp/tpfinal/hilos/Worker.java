package ar.edu.unc.fcefyn.pcp.tpfinal.hilos;

import ar.edu.unc.fcefyn.pcp.tpfinal.monitor.MonitorInterface;

public abstract class Worker implements Runnable {
    private final MonitorInterface monitor;
    private final int[] transitionsSequence;
    private final long[] alphaTimes;

    public Worker(MonitorInterface monitor, int[] transitionsSequence, long[] alphaTimes) {
        this.monitor = monitor;
        this.transitionsSequence = transitionsSequence;
        this.alphaTimes = alphaTimes;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            for (int t : transitionsSequence) {
                // Duerme fuera del monitor antes de pedir el disparo.
                // Si alphaTimes = null, sigue de largo sin dormir.
                if (alphaTimes != null && alphaTimes[t] > 0) {
                    try {
                        Thread.sleep(alphaTimes[t]);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                // Cuando T9 llega a 200, el monitor despierta a todos y devuelve false
                if (!monitor.fireTransition(t)) {
                    return;
                }
            }
        }
    }
}
