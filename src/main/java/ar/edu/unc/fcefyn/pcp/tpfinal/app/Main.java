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
import java.util.Locale;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        // Los args se validan antes de crear el Logger
        Politica politica;
        try {
            politica = crearPolitica(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Uso: Main [random | priority]");
            System.exit(64);
            return;
        }
        String etiqueta = "random";
        if (politica instanceof PriorityPolicy ) {
            etiqueta = "priority";
        }

        String fecha = LocalDateTime.now() //Obtiene la fecha y hora actual y la formatea como "yyyy-MM-dd_HH-mm-ss-SSS"
            .format(DateTimeFormatter.ofPattern("uuuu-MM-dd_HH-mm-ss-SSS"));
        String rutaLog = "resultados/logs/ejecucion_" + etiqueta + "_" + fecha + ".log"; //Une la carpeta, la etiqueta, la fecha y la extension
        ConfigPagos config = new ConfigPagos(rutaLog);
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

        MonitorInterface monitor = new Monitor(redPetri, politica, logger, config.getAlphaTimes());

        List<Thread> hilos = new ArrayList<>();

        // 3 hilos de Admisión (T0)
        for (int i = 0; i < 3; i++) {
            hilos.add(new Thread(new AdmisionWorker(monitor), "Admision-" + i));
        }

        // 1 hilo por cada rama de procesamiento
        hilos.add(new Thread(new TarjetaWorker(monitor), "Tarjeta-0"));
        hilos.add(new Thread(new AltoRiesgoWorker(monitor), "AltoRiesgo-0"));
        hilos.add(new Thread(new TransferenciaWorker(monitor), "Transferencia-0"));

        // 3 hilos de Salida (T9)
        for (int i = 0; i < 3; i++) {
            hilos.add(new Thread(new SalidaWorker(monitor), "Salida-" + i));
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
        // Un solo procesamiento del log: validez (exigiendo el total objetivo) + conteos
        InvariantChecker.ResultadoConteo res =
                InvariantChecker.procesarLog(config.getLogFilePath(), config.getTargetInvariants());

        System.out.println("Red finalizada (T9 == " + config.getTargetInvariants() + "): " + redPetri.isFinished());
        System.out.println("Invariantes de transición válidos: " + res.valido);
        if (!res.valido) {
            System.out.println("  Total completado: " + res.getTotalInvariantes()
                    + " (esperado " + config.getTargetInvariants() + ")");
            System.out.println("  Transiciones sin reducir: '" + res.resto + "'");
        }
        System.out.println("Distribución por invariante (total " + res.getTotalInvariantes() + "):");
        System.out.printf("  Tarjeta       = %3d (%6.2f%%)%n", res.tarjeta, res.getPorcentajeTarjeta());
        System.out.printf("  Alto Riesgo   = %3d (%6.2f%%)%n", res.altoRiesgo, res.getPorcentajeAltoRiesgo());
        System.out.printf("  Transferencia = %3d (%6.2f%%)%n", res.transferencia, res.getPorcentajeTransferencia());
        System.out.println("Todos los " + hilos.size() + " hilos terminaron en " + duracionMs + " ms.");

        // Código de salida 2 si el log no es válido (no se redujo por completo o el total no coincide)
        if (!res.valido) {
            System.exit(2);
        }
    }

    /**
    * Crea la política indicada por los argumentos.
    * Sin argumentos utiliza random.
    */
    private static Politica crearPolitica(String[] args) {
        if (args.length > 1) {
            throw new IllegalArgumentException(
                "Se admite un solo argumento: random o priority"
            );
        }
        String nombre = "random";
        if (args.length == 1) {
            nombre = args[0].toLowerCase(Locale.ROOT);
        }

        switch (nombre) {
            case "random":
                return new RandomPolicy();
            case "priority":
                return new PriorityPolicy();
            default:
                throw new IllegalArgumentException("Política desconocida: " + nombre);
        }
    }
}
