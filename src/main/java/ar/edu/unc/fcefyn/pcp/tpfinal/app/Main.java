package ar.edu.unc.fcefyn.pcp.tpfinal.app;

import ar.edu.unc.fcefyn.pcp.tpfinal.hilos.*;
import ar.edu.unc.fcefyn.pcp.tpfinal.monitor.Monitor;
import ar.edu.unc.fcefyn.pcp.tpfinal.monitor.MonitorInterface;
import ar.edu.unc.fcefyn.pcp.tpfinal.politica.Politica;
import ar.edu.unc.fcefyn.pcp.tpfinal.politica.PriorityPolicy;
import ar.edu.unc.fcefyn.pcp.tpfinal.politica.RandomPolicy;
import ar.edu.unc.fcefyn.pcp.tpfinal.red.ConfigPagos;
import ar.edu.unc.fcefyn.pcp.tpfinal.red.RedPetri;
import ar.edu.unc.fcefyn.pcp.tpfinal.registro.InvariantChecker;
import ar.edu.unc.fcefyn.pcp.tpfinal.registro.Logger;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        ConfigPagos config = new ConfigPagos();
        // El logger se crea antes del handler para poder cerrarlo si un hilo muere
        Logger logger = new Logger(config.getLogFilePath());

        // Si un hilo muere por excepción el programa se detiene y no se queda colgado
        Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> {
            System.err.println("Error fatal en el hilo " + hilo.getName() + ": " + error);
            error.printStackTrace();
            try {
                logger.close(); // vuelca el buffer: conserva la traza hasta el error
            } catch (RuntimeException e) {
                System.err.println("No se pudo cerrar el log: " + e.getMessage());
            }
            System.exit(1);
        });

        RedPetri redPetri = new RedPetri(config);
        Politica politica = crearPolitica(args);

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
        InvariantChecker.ResultadoConteo resultado = InvariantChecker.procesarLog(config.getLogFilePath());

        System.out.println("Red finalizada (T9 == 200): " + redPetri.isFinished());
        System.out.println("Invariantes de transición válidos: " + resultado.valido);
        System.out.println("Tarjeta=" + resultado.tarjeta + ", AltoRiesgo=" + resultado.altoRiesgo
                + ", Transferencia=" + resultado.transferencia);
        System.out.println("Todos los " + hilos.size() + " hilos terminaron en " + duracionMs + " ms.");

        // Código de salida 2 si el log no se redujo por completo a invariantes válidos
        if (!resultado.valido) {
            System.err.println("Invariantes de transición inválidos. Resto sin reducir: " + resultado.resto);
            System.exit(2);
        }
    }

    private static Politica crearPolitica(String[] args) {
        String nombre = args.length > 0 ? args[0].toLowerCase() : "random";
        switch (nombre) {
            case "random":
                return new RandomPolicy();
            case "priority":
                double prob = 0.8; // ver si hace falta cambiar
                if (args.length > 1) {
                    try {
                        prob = Double.parseDouble(args[1]);
                    } catch (NumberFormatException e) {
                        System.err.println("Probabilidad inválida: " + args[1]);
                        System.exit(64);
                    }
                }
                return new PriorityPolicy(prob);
            default:
                System.err.println("Política desconocida: " + args[0]
                        + ". Uso: <random|priority [probabilidad]>");
                System.exit(64);
                return null; // inalcanzable
        }
    }
}
