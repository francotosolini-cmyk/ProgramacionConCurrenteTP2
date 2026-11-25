package ar.edu.unc.fcefyn.pcp.tpfinal.hilos;

import ar.edu.unc.fcefyn.pcp.tpfinal.monitor.MonitorInterface;

// Se instancia 1 hilo de esta clase
public class TransferenciaWorker extends Worker {
    public TransferenciaWorker(MonitorInterface monitor) {
        super(monitor, new int[]{6, 7, 8});
    }
}
