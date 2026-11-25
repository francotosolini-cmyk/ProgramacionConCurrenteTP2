package ar.edu.unc.fcefyn.pcp.tpfinal.hilos;

import ar.edu.unc.fcefyn.pcp.tpfinal.monitor.MonitorInterface;

public abstract class Worker implements Runnable {
    private final MonitorInterface monitor;
    private final int[] transitionsSequence;

    public Worker(MonitorInterface monitor, int[] transitionsSequence) {
        this.monitor = monitor;
        this.transitionsSequence = transitionsSequence;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            for (int t : transitionsSequence) {
                // Cuando T9 llega a 200, el monitor despierta a todos y devuelve false
                if (!monitor.fireTransition(t)) {
                    return;
                }
            }
        }
    }
}
