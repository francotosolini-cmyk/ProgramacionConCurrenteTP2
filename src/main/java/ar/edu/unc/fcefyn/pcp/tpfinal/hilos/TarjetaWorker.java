package ar.edu.unc.fcefyn.pcp.tpfinal.hilos;

import ar.edu.unc.fcefyn.pcp.tpfinal.monitor.MonitorInterface;

// Se instancia 1 hilo de esta clase
public class TarjetaWorker extends Worker {
    public TarjetaWorker(MonitorInterface monitor) {
        super(monitor, new int[]{1, 2, 3});
    }
}
