package ar.edu.unc.fcefyn.pcp.tpfinal.red;

/**
 * Configuración específica de la Red de Petri para el sistema de procesamiento de pagos.
 * Centraliza la definición de matrices de incidencia, marcado inicial, tiempos temporales,
 * invariantes de plaza y parámetros de finalización.
 * Es la única clase que conoce la topología concreta de esta red.
 */
public class ConfigPagos {

    public static final int PLAZAS_COUNT = 10;
    public static final int TRANSICIONES_COUNT = 10;

    // Plazas
    public static final int P0 = 0; // Cola de arribo (IDLE)
    public static final int P1 = 1; // Transacción admitida
    public static final int P2 = 2; // Tarjeta: autorización
    public static final int P3 = 3; // Tarjeta: captura
    public static final int P4 = 4; // Alto riesgo: scoring
    public static final int P5 = 5; // Transferencia: validación
    public static final int P6 = 6; // Transferencia: ejecución
    public static final int P7 = 7; // Recurso: Gateway de tarjetas
    public static final int P8 = 8; // Recurso: Slot motor antifraude
    public static final int P9 = 9; // Buffer de salida

    // Transiciones
    public static final int T0 = 0; // Admisión
    public static final int T1 = 1; // Ruteo tarjeta
    public static final int T2 = 2; // Autorización completada (temporal)
    public static final int T3 = 3; // Captura completada y libera gateway (temporal)
    public static final int T4 = 4; // Ruteo alto riesgo
    public static final int T5 = 5; // Scoring completado y libera ambos recursos (temporal)
    public static final int T6 = 6; // Ruteo transferencia
    public static final int T7 = 7; // Validación completada (temporal)
    public static final int T8 = 8; // Transferencia completada y libera motor (temporal)
    public static final int T9 = 9; // Salida / confirmación a cliente

    private final int[][] iMinus;
    private final int[][] iPlus;
    private final int[] marcadoInicial;
    private final long[] alphaTimes;
    private final int[][] placeInvariants;
    private final int[] placeConstants;
    private final int entryTransition;
    private final int exitTransition;
    private final int targetInvariants;
    private final String logFilePath;

    /**
     * Constructor por defecto con la configuración estándar del sistema de pagos.
     * Los tiempos alfa asignados están calibrados para que 200 invariantes demoren
     * entre 20 y 40 segundos según el requerimiento 10.
     */
    public ConfigPagos() {
        this("resultados/logs/ejecucion.log", new long[] {
            0,    // T0: inmediata
            0,    // T1: inmediata
            50,   // T2: temporal (autorización)
            50,   // T3: temporal (captura)
            0,    // T4: inmediata
            120,  // T5: temporal (scoring alto riesgo)
            0,    // T6: inmediata
            50,   // T7: temporal (validación cuenta)
            50,   // T8: temporal (ejecución bancaria)
            0     // T9: inmediata
        });
    }

    /**
     * Constructor que permite parametrizar la ruta de log y los tiempos alfa.
     */
    public ConfigPagos(String logFilePath, long[] alphaTimes) {
        this.logFilePath = logFilePath;
        this.targetInvariants = 200;
        this.entryTransition = T0;
        this.exitTransition = T9;

        if (alphaTimes == null || alphaTimes.length != TRANSICIONES_COUNT) {
            throw new IllegalArgumentException("alphaTimes debe tener longitud " + TRANSICIONES_COUNT);
        }
        this.alphaTimes = alphaTimes.clone();

        // Marcado inicial: 3 tokens en P0, 1 en P7 y 1 en P8
        this.marcadoInicial = new int[PLAZAS_COUNT];
        this.marcadoInicial[P0] = 3;
        this.marcadoInicial[P7] = 1;
        this.marcadoInicial[P8] = 1;

        // Matriz I- (consumo: plazas x transiciones)
        this.iMinus = new int[PLAZAS_COUNT][TRANSICIONES_COUNT];
        this.iMinus[P0][T0] = 1;

        this.iMinus[P1][T1] = 1;
        this.iMinus[P7][T1] = 1;

        this.iMinus[P2][T2] = 1;

        this.iMinus[P3][T3] = 1;

        this.iMinus[P1][T4] = 1;
        this.iMinus[P7][T4] = 1;
        this.iMinus[P8][T4] = 1;

        this.iMinus[P4][T5] = 1;

        this.iMinus[P1][T6] = 1;
        this.iMinus[P8][T6] = 1;

        this.iMinus[P5][T7] = 1;

        this.iMinus[P6][T8] = 1;

        this.iMinus[P9][T9] = 1;

        // Matriz I+ (producción: plazas x transiciones)
        this.iPlus = new int[PLAZAS_COUNT][TRANSICIONES_COUNT];
        this.iPlus[P1][T0] = 1;

        this.iPlus[P2][T1] = 1;

        this.iPlus[P3][T2] = 1;

        this.iPlus[P7][T3] = 1;
        this.iPlus[P9][T3] = 1;

        this.iPlus[P4][T4] = 1;

        this.iPlus[P7][T5] = 1;
        this.iPlus[P8][T5] = 1;
        this.iPlus[P9][T5] = 1;

        this.iPlus[P5][T6] = 1;

        this.iPlus[P6][T7] = 1;

        this.iPlus[P8][T8] = 1;
        this.iPlus[P9][T8] = 1;

        this.iPlus[P0][T9] = 1;

        // Invariantes de plaza (3 invariantes):
        // 1) Gateway: P2 + P3 + P4 + P7 = 1
        // 2) Motor Antifraude: P4 + P5 + P6 + P8 = 1
        // 3) Conservación de transacciones: P0 + P1 + P2 + P3 + P4 + P5 + P6 + P9 = 3
        this.placeInvariants = new int[][] {
            {0, 0, 1, 1, 1, 0, 0, 1, 0, 0}, // Gateway
            {0, 0, 0, 0, 1, 1, 1, 0, 1, 0}, // Antifraude
            {1, 1, 1, 1, 1, 1, 1, 0, 0, 1}  // Transacciones totales
        };
        this.placeConstants = new int[] {1, 1, 3};
    }

    public int[][] getIMinus() {
        return cloneMatrix(iMinus);
    }

    public int[][] getIPlus() {
        return cloneMatrix(iPlus);
    }

    public int[] getMarcadoInicial() {
        return marcadoInicial.clone();
    }

    public long[] getAlphaTimes() {
        return alphaTimes.clone();
    }

    public int[][] getPlaceInvariants() {
        return cloneMatrix(placeInvariants);
    }

    public int[] getPlaceConstants() {
        return placeConstants.clone();
    }

    public int getEntryTransition() {
        return entryTransition;
    }

    public int getExitTransition() {
        return exitTransition;
    }

    public int getTargetInvariants() {
        return targetInvariants;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    private static int[][] cloneMatrix(int[][] matrix) {
        int[][] copy = new int[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            copy[i] = matrix[i].clone();
        }
        return copy;
    }
}
