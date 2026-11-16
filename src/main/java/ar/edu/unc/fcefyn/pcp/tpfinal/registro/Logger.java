package ar.edu.unc.fcefyn.pcp.tpfinal.registro;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Registrador de disparos de la Red de Petri.
 * Escribe de forma secuencial cada disparo y su instante temporal en un archivo de log.
 * No requiere ser 'synchronized' dado que el Monitor siempre lo invoca con el mutex tomado,
 * garantizando que el orden del archivo coincida exactamente con el orden temporal de los disparos.
 */
public class Logger {

    private final String logFilePath;
    private final BufferedWriter writer;

    /**
     * Constructor protegido sin argumentos para dobles de prueba (test mocks).
     */
    protected Logger() {
        this.logFilePath = null;
        this.writer = null;
    }

    /**
     * Inicializa el logger y abre el archivo especificado con un BufferedWriter.
     * El archivo permanece abierto durante toda la simulación para evitar el overhead
     * de I/O que alteraría las mediciones temporales.
     *
     * @param logFilePath Ruta del archivo de log.
     */
    public Logger(String logFilePath) {
        if (logFilePath == null || logFilePath.isBlank()) {
            throw new IllegalArgumentException("La ruta del archivo de log no puede ser vacía");
        }
        this.logFilePath = logFilePath;
        try {
            Path path = Paths.get(logFilePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            this.writer = new BufferedWriter(new FileWriter(logFilePath, StandardCharsets.UTF_8, false));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el archivo de log en: " + logFilePath, e);
        }
    }

    /**
     * Escribe en el archivo de log la transición disparada y el instante temporal.
     *
     * @param t Índice de la transición disparada (0 a 9).
     */
    public void logTransition(int t) {
        if (writer == null) {
            return;
        }
        try {
            writer.write("T" + t + " " + System.currentTimeMillis());
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Error al registrar el disparo de T" + t + " en el log", e);
        }
    }

    /**
     * Vacía el buffer y cierra el archivo de log.
     * Debe ser invocado por Main una vez que todos los hilos hayan finalizado (join).
     */
    public void close() {
        if (writer == null) {
            return;
        }
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Error al cerrar el archivo de log: " + logFilePath, e);
        }
    }

    public String getLogFilePath() {
        return logFilePath;
    }
}
