package ar.edu.unc.fcefyn.pcp.tpfinal.hilos;

import ar.edu.unc.fcefyn.pcp.tpfinal.monitor.MonitorInterface;

// Se instancia 1 hilo de esta clase
public class AltoRiesgoWorker extends Worker {
    public AltoRiesgoWorker(MonitorInterface monitor) {
        super(monitor, new int[]{4, 5});
    }
}
