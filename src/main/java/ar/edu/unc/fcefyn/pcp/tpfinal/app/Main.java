package ar.edu.unc.fcefyn.pcp.tpfinal.app;

import ar.edu.unc.fcefyn.pcp.tpfinal.hilos.*;
import ar.edu.unc.fcefyn.pcp.tpfinal.monitor.Monitor;
import ar.edu.unc.fcefyn.pcp.tpfinal.monitor.MonitorInterface;
import ar.edu.unc.fcefyn.pcp.tpfinal.politica.Politica;
import ar.edu.unc.fcefyn.pcp.tpfinal.politica.RandomPolicy;
import ar.edu.unc.fcefyn.pcp.tpfinal.red.ConfigPagos;
import ar.edu.unc.fcefyn.pcp.tpfinal.red.RedPetri;
import ar.edu.unc.fcefyn.pcp.tpfinal.registro.InvariantChecker;
import ar.edu.unc.fcefyn.pcp.tpfinal.registro.Logger;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Si un hilo muere por excepción el programa se detiene y no se queda colgado
        Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> {
            System.err.println("Error fatal en el hilo " + hilo.getName() + ": " + error);
            error.printStackTrace();
            System.exit(1);
        });

        ConfigPagos config = new ConfigPagos();
        RedPetri redPetri = new RedPetri(config);
        Politica politica = new RandomPolicy();

        Logger logger = new Logger(config.getLogFilePath());
        MonitorInterface monitor = new Monitor(redPetri, politica, logger);

        // null p/ correr sin tiempos. Después cambiar null por config.getAlphaTimes()
        long[] alphaTimes = null;

        List<Thread> hilos = new ArrayList<>();

        // 3 hilos de Admisión (T0)
        for (int i = 0; i < 3; i++) {
            hilos.add(new Thread(new AdmisionWorker(monitor, alphaTimes), "Admision-" + i));
        }

        // 1 hilo por cada rama de procesamiento
        hilos.add(new Thread(new TarjetaWorker(monitor, alphaTimes), "Tarjeta-0"));
        hilos.add(new Thread(new AltoRiesgoWorker(monitor, alphaTimes), "AltoRiesgo-0"));
        hilos.add(new Thread(new TransferenciaWorker(monitor, alphaTimes), "Transferencia-0"));

        // 3 hilos de Salida (T9)
        for (int i = 0; i < 3; i++) {
            hilos.add(new Thread(new SalidaWorker(monitor, alphaTimes), "Salida-" + i));
        }

        long inicio = System.currentTimeMillis();

        for (Thread h : hilos) {
            h.start();
        }

        for (Thread h : hilos) {
            h.join();
        }

        long duracionMs = System.currentTimeMillis() - inicio;

        // cerrar logger (vuelca el buffer) y validar invariantes sobre el log
        logger.close();
        boolean ok = InvariantChecker.verifyTransitionInvariants(config.getLogFilePath());
        int[] porInvariante = InvariantChecker.countByInvariant(config.getLogFilePath());

        System.out.println("Red finalizada (T9 == 200): " + redPetri.isFinished());
        System.out.println("Invariantes de transición válidos: " + ok);
        System.out.println("Tarjeta=" + porInvariante[0] + ", AltoRiesgo=" + porInvariante[1]
                + ", Transferencia=" + porInvariante[2]);
        System.out.println("Todos los " + hilos.size() + " hilos terminaron en " + duracionMs + " ms.");
    }
}