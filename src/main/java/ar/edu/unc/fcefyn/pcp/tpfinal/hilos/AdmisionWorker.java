package ar.edu.unc.fcefyn.pcp.tpfinal.hilos;

import ar.edu.unc.fcefyn.pcp.tpfinal.monitor.MonitorInterface;

// Se instancian 3 hilos de esta clase
public class AdmisionWorker extends Worker {
    public AdmisionWorker(MonitorInterface monitor, long[] alphaTimes) {
        super(monitor, new int[]{0}, alphaTimes);
    }
}
