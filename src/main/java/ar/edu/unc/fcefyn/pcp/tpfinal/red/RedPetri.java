package ar.edu.unc.fcefyn.pcp.tpfinal.red;

import java.util.Arrays;

/**
 * Representa el estado y la lógica de la Red de Petri.
 * Mantiene el marcado actual, evalúa la sensibilización y aplica los disparos.
 * Es agnóstica a la concurrencia: no maneja semáforos, hilos ni tiempos,
 * delegando esa responsabilidad en el Monitor y los Workers.
 */
public class RedPetri {

    private final int plazasCount;
    private final int transicionesCount;

    private final int[][] I; // Matriz combinada I = I+ - I-
    private final int[][] iMinus; // Matriz I- de precondiciones
    private final int[] marcado; // Vector de marcado actual
    private final int[] fireCount; // Contador de disparos por transición

    private final int[][] placeInvariants;
    private final int[] placeConstants;
    private final int entryTransition;
    private final int exitTransition;
    private final int targetInvariants;

    /**
     * Construye la Red de Petri a partir de los parámetros provistos por ConfigPagos.
     *
     * @param config Configuración de la red con matrices, marcado e invariantes.
     */
    public RedPetri(ConfigPagos config) {
        if (config == null) {
            throw new IllegalArgumentException("La configuración no puede ser nula");
        }

        this.iMinus = config.getIMinus();
        int[][] iPlus = config.getIPlus();

        this.plazasCount = iMinus.length;
        this.transicionesCount = iMinus[0].length;

        // Calcular matriz combinada I = I+ - I-
        this.I = new int[plazasCount][transicionesCount];
        for (int p = 0; p < plazasCount; p++) {
            for (int t = 0; t < transicionesCount; t++) {
                this.I[p][t] = iPlus[p][t] - this.iMinus[p][t];
            }
        }

        this.marcado = config.getMarcadoInicial();
        this.fireCount = new int[transicionesCount];

        this.placeInvariants = config.getPlaceInvariants();
        this.placeConstants = config.getPlaceConstants();
        this.entryTransition = config.getEntryTransition();
        this.exitTransition = config.getExitTransition();
        this.targetInvariants = config.getTargetInvariants();

        // Control defensivo inicial: verificar que M0 cumple los invariantes de plaza
        checkPlaceInvariants();
    }

    /**
     * Evalúa si una transición específica se encuentra sensibilizada en el marcado actual.
     * Para la transición de entrada (T0), deja de sensibilizarse una vez alcanzado el objetivo
     * de admisiones (targetInvariants = 200), evitando transacciones adicionales.
     *
     * @param t Índice de la transición.
     * @return true si la transición puede dispararse, false en caso contrario.
     */
    public boolean isSensitised(int t) {
        if (t < 0 || t >= transicionesCount) {
            return false;
        }

        // Freno de admisión al alcanzar la cantidad objetivo de transacciones
        if (t == entryTransition && fireCount[t] >= targetInvariants) {
            return false;
        }

        // Verificar que cada plaza de entrada posea suficientes tokens
        for (int p = 0; p < plazasCount; p++) {
            if (marcado[p] < iMinus[p][t]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Obtiene el vector de sensibilización para todas las transiciones de la red.
     * Es utilizado por la política para determinar qué transiciones candidatas pueden ser elegidas.
     *
     * @return boolean[] donde cada posición t indica si la transición t está sensibilizada.
     */
    public boolean[] getSensitisedTransitions() {
        boolean[] sensitised = new boolean[transicionesCount];
        for (int t = 0; t < transicionesCount; t++) {
            sensitised[t] = isSensitised(t);
        }
        return sensitised;
    }

    /**
     * Ejecuta el disparo de una transición sobre la red.
     * Actualiza el marcado sumando la columna correspondiente de la matriz I,
     * incrementa el contador de disparos y verifica los invariantes de plaza (Req. 12).
     *
     * @param t Índice de la transición a disparar.
     * @return true si el disparo fue exitoso, false si la transición no estaba sensibilizada.
     */
    public boolean fire(int t) {
        if (!isSensitised(t)) {
            return false; // Control defensivo
        }

        // Aplicar ecuación fundamental de estado: M' = M + I * delta(t)
        for (int p = 0; p < plazasCount; p++) {
            marcado[p] += I[p][t];
        }

        fireCount[t]++;

        // Verificación estricta del Requerimiento 12 tras cada disparo
        checkPlaceInvariants();

        return true;
    }

    /**
     * Indica si el sistema ha completado el procesamiento de todas las transacciones objetivo.
     * Se considera finalizado cuando la transición de salida (T9) alcanza los 200 disparos.
     *
     * @return true si finalizó la red, false si aún quedan transacciones en proceso.
     */
    public boolean isFinished() {
        return fireCount[exitTransition] >= targetInvariants;
    }

    /**
     * Verifica que el marcado actual respete todos los invariantes de plaza definidos.
     * Lanza IllegalStateException en caso de detectar alguna inconsistencia (Req. 12).
     *
     * @return true si todos los invariantes se cumplen.
     */
    private boolean checkPlaceInvariants() {
        for (int i = 0; i < placeInvariants.length; i++) {
            int sumaTokens = 0;
            for (int p = 0; p < plazasCount; p++) {
                sumaTokens += placeInvariants[i][p] * marcado[p];
            }

            if (sumaTokens != placeConstants[i]) {
                throw new IllegalStateException(String.format(
                    "Violación del invariante de plaza #%d: esperado=%d, obtenido=%d. Marcado actual: %s",
                    i, placeConstants[i], sumaTokens, Arrays.toString(marcado)
                ));
            }
        }
        return true;
    }

    public int getTransicionesCount() {
        return transicionesCount;
    }

    public int getPlazasCount() {
        return plazasCount;
    }

    public int[] getMarcadoActual() {
        return marcado.clone();
    }

    public int[] getFireCount() {
        return fireCount.clone();
    }

    public int getFireCount(int t) {
        if (t < 0 || t >= transicionesCount) {
            throw modernIndexException(t);
        }
        return fireCount[t];
    }

    private IndexOutOfBoundsException modernIndexException(int t) {
        return new IndexOutOfBoundsException("Transición inválida: " + t);
    }
}
