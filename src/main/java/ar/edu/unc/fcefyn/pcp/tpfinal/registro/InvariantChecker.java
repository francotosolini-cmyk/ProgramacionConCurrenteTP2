package ar.edu.unc.fcefyn.pcp.tpfinal.registro;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verificador y analizador post-ejecución de Invariantes de Transición (T-invariantes).
 * Procesa el archivo de log generado por la simulación y aplica expresiones regulares
 * para certificar que todos los disparos pertenezcan a invariantes válidos (Requerimiento 13)
 * y cuantificar la distribución de transacciones por flujo (Requerimiento 8).
 */
public class InvariantChecker {

    // Regex para Invariante 1 (Tarjeta: T0 T1 T2 T3 T9)
    // Captura y preserva cualquier disparo intercalado entre sus etapas
    private static final Pattern P_TARJETA = Pattern.compile(
        "\\bT0\\b((?:(?!\\bT0\\b).)*?)\\bT1\\b((?:(?!\\bT2\\b).)*?)\\bT2\\b((?:(?!\\bT3\\b).)*?)\\bT3\\b((?:(?!\\bT9\\b).)*?)\\bT9\\b"
    );

    // Regex para Invariante 2 (Alto Riesgo: T0 T4 T5 T9)
    private static final Pattern P_ALTO_RIESGO = Pattern.compile(
        "\\bT0\\b((?:(?!\\bT0\\b).)*?)\\bT4\\b((?:(?!\\bT5\\b).)*?)\\bT5\\b((?:(?!\\bT9\\b).)*?)\\bT9\\b"
    );

    // Regex para Invariante 3 (Transferencia: T0 T6 T7 T8 T9)
    private static final Pattern P_TRANSFERENCIA = Pattern.compile(
        "\\bT0\\b((?:(?!\\bT0\\b).)*?)\\bT6\\b((?:(?!\\bT7\\b).)*?)\\bT7\\b((?:(?!\\bT8\\b).)*?)\\bT8\\b((?:(?!\\bT9\\b).)*?)\\bT9\\b"
    );

    /**
     * Lee la traza de transiciones del archivo de log y corrobora mediante expresiones regulares
     * que todas las transiciones formen invariantes de ciclo completos (Req. 13).
     *
     * @param logFilePath Ruta al archivo de log.
     * @return true si la traza se redujo completamente a vacío y todos los invariantes cerraron correctamente.
     */
    public static boolean verifyTransitionInvariants(String logFilePath) {
        ResultadoConteo resultado = procesarLog(logFilePath);
        return resultado.valido;
    }

    /**
     * Devuelve la cantidad de veces que se completó cada uno de los tres invariantes de transición (Req. 8).
     *
     * @param logFilePath Ruta al archivo de log.
     * @return int[] de 3 posiciones:
     *         [0]: Tarjeta (T0 T1 T2 T3 T9)
     *         [1]: Alto Riesgo (T0 T4 T5 T9)
     *         [2]: Transferencia (T0 T6 T7 T8 T9)
     */
    public static int[] countByInvariant(String logFilePath) {
        ResultadoConteo resultado = procesarLog(logFilePath);
        return new int[] { resultado.tarjeta, resultado.altoRiesgo, resultado.transferencia };
    }

    /**
     * Procesa íntegramente el log devolviendo los conteos y el estado final de reducción.
     */
    public static ResultadoConteo procesarLog(String logFilePath) {
        String secuencia = leerSecuenciaTransiciones(logFilePath);
        return reducirSecuencia(secuencia);
    }

    /**
     * Procesa el log y además exige que la cantidad total de invariantes completados
     * sea exactamente la esperada (ej. 200, Req. 8).
     *
     * @param logFilePath Ruta al archivo de log.
     * @param totalEsperado Cantidad de invariantes que debe haber completado la simulación.
     */
    public static ResultadoConteo procesarLog(String logFilePath, int totalEsperado) {
        String secuencia = leerSecuenciaTransiciones(logFilePath);
        return reducirSecuencia(secuencia, totalEsperado);
    }

    /**
     * Lee las transiciones registradas en el archivo de log y construye una cadena de tokens separados por espacio.
     */
    public static String leerSecuenciaTransiciones(String logFilePath) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] partes = line.split("\\s+");
                if (partes.length > 0 && partes[0].startsWith("T")) {
                    sb.append(partes[0]).append(" ");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo de log en: " + logFilePath, e);
        }
        return sb.toString();
    }

    /**
     * Reduce iterativamente la secuencia de transiciones eliminando invariantes completos
     * mientras preserva las transiciones que puedan estar intercaladas.
     *
     * @param secuencia Cadena de transiciones (ej: "T0 T1 T2 T3 T9 ...").
     * @return ResultadoConteo con el detalle de reducción y conteos.
     */
    public static ResultadoConteo reducirSecuencia(String secuencia) {
        return reducirSecuencia(secuencia, -1);
    }

    /**
     * Igual que {@link #reducirSecuencia(String)}, pero si {@code totalEsperado >= 0}
     * la traza solo es válida cuando el total de invariantes completados coincide exactamente
     * con ese valor.
     *
     * @param secuencia Cadena de transiciones.
     * @param totalEsperado Total de invariantes exigido, o un valor negativo para no exigirlo.
     */
    public static ResultadoConteo reducirSecuencia(String secuencia, int totalEsperado) {
        String s = secuencia;
        int countTarjeta = 0;
        int countAltoRiesgo = 0;
        int countTransferencia = 0;

        boolean redujo = true;
        while (redujo) {
            redujo = false;

            Matcher mTarjeta = P_TARJETA.matcher(s);
            if (mTarjeta.find()) {
                s = mTarjeta.replaceFirst("$1$2$3$4");
                countTarjeta++;
                redujo = true;
                continue;
            }

            Matcher mAltoRiesgo = P_ALTO_RIESGO.matcher(s);
            if (mAltoRiesgo.find()) {
                s = mAltoRiesgo.replaceFirst("$1$2$3");
                countAltoRiesgo++;
                redujo = true;
                continue;
            }

            Matcher mTransferencia = P_TRANSFERENCIA.matcher(s);
            if (mTransferencia.find()) {
                s = mTransferencia.replaceFirst("$1$2$3$4");
                countTransferencia++;
                redujo = true;
                continue;
            }
        }

        String resto = s.trim();
        int totalInvariantes = countTarjeta + countAltoRiesgo + countTransferencia;
        boolean valido = resto.isEmpty() && totalInvariantes > 0
                && (totalEsperado < 0 || totalInvariantes == totalEsperado);

        return new ResultadoConteo(valido, countTarjeta, countAltoRiesgo, countTransferencia, resto);
    }

    /**
     * Estructura inmutable que transporta el resultado detallado del análisis de invariantes.
     */
    public static class ResultadoConteo {
        public final boolean valido;
        public final int tarjeta;
        public final int altoRiesgo;
        public final int transferencia;
        public final String resto;

        public ResultadoConteo(boolean valido, int tarjeta, int altoRiesgo, int transferencia, String resto) {
            this.valido = valido;
            this.tarjeta = tarjeta;
            this.altoRiesgo = altoRiesgo;
            this.transferencia = transferencia;
            this.resto = resto;
        }

        public int getTotalInvariantes() {
            return tarjeta + altoRiesgo + transferencia;
        }

        /**
         * Porcentaje que representa una cantidad sobre el total de invariantes completados.
         * Devuelve 0 si no se completó ninguno.
         */
        private double porcentaje(int cantidad) {
            int total = getTotalInvariantes();
            return total == 0 ? 0.0 : cantidad * 100.0 / total;
        }

        public double getPorcentajeTarjeta() {
            return porcentaje(tarjeta);
        }

        public double getPorcentajeAltoRiesgo() {
            return porcentaje(altoRiesgo);
        }

        public double getPorcentajeTransferencia() {
            return porcentaje(transferencia);
        }

        @Override
        public String toString() {
            return String.format(
                "ResultadoConteo[valido=%b, tarjeta=%d (%.2f%%), altoRiesgo=%d (%.2f%%), "
                    + "transferencia=%d (%.2f%%), total=%d, resto='%s']",
                valido, tarjeta, getPorcentajeTarjeta(), altoRiesgo, getPorcentajeAltoRiesgo(),
                transferencia, getPorcentajeTransferencia(), getTotalInvariantes(), resto
            );
        }
    }
}
