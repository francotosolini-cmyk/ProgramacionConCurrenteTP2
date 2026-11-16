package ar.edu.unc.fcefyn.pcp.tpfinal.hilos;

import ar.edu.unc.fcefyn.pcp.tpfinal.monitor.MonitorInterface;

// Se instancian 3 hilos de esta clase
public class SalidaWorker extends Worker {
    public SalidaWorker(MonitorInterface monitor, long[] alphaTimes) {
        super(monitor, new int[]{9}, alphaTimes);
    }
}
