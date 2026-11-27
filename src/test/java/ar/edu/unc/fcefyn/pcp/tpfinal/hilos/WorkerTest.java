package ar.edu.unc.fcefyn.pcp.tpfinal.hilos;

import ar.edu.unc.fcefyn.pcp.tpfinal.monitor.MonitorInterface;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkerTest {

    private static class MonitorGrabador implements MonitorInterface {
        final List<Integer> disparos = new ArrayList<>();
        private final int limite;

        MonitorGrabador(int limite) {
            this.limite = limite;
        }

        @Override
        public boolean fireTransition(int t) {
            if (disparos.size() >= limite) {
                return false;
            }
            disparos.add(t);
            return true;
        }
    }

    @Test
    void cadaWorkerDisparaSuSecuenciaHastaQueElMonitorDevuelveFalse() {
        MonitorGrabador adm = new MonitorGrabador(3);
        new AdmisionWorker(adm).run();
        assertEquals(List.of(0, 0, 0), adm.disparos);

        MonitorGrabador tar = new MonitorGrabador(4);
        new TarjetaWorker(tar).run();
        assertEquals(List.of(1, 2, 3, 1), tar.disparos);

        MonitorGrabador ar = new MonitorGrabador(3);
        new AltoRiesgoWorker(ar).run();
        assertEquals(List.of(4, 5, 4), ar.disparos);

        MonitorGrabador tr = new MonitorGrabador(4);
        new TransferenciaWorker(tr).run();
        assertEquals(List.of(6, 7, 8, 6), tr.disparos);

        MonitorGrabador sal = new MonitorGrabador(2);
        new SalidaWorker(sal).run();
        assertEquals(List.of(9, 9), sal.disparos);
    }

    @Test
    void workerTerminaSiElHiloEstaInterrumpido() {
        MonitorGrabador monitor = new MonitorGrabador(10);
        Thread.currentThread().interrupt();
        try {
            new AdmisionWorker(monitor).run();
            assertEquals(0, monitor.disparos.size());
        } finally {
            Thread.interrupted();
        }
    }
}
