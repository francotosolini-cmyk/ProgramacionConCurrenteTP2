package ar.edu.unc.fcefyn.pcp.tpfinal.monitor;

public class ControlTemporal {

    /** β = infinito: no existe DESPUES_DE_VENTANA. */
    public enum ResultadoVentana { NO_TEMPORAL, ANTES_DE_VENTANA, DENTRO_DE_VENTANA }

    private final long[] alphaMs;               // 0 = transición inmediata
    private final long[] sensibilizadaDesdeNs;  // timestamp de sensibilización
    private final boolean[] esperandoVentana;   // alguien duerme la ventana de t

    public ControlTemporal(long[] alphaMs, boolean[] sensibilizadasIniciales) {
        int n = sensibilizadasIniciales.length;
        this.alphaMs = alphaMs != null ? alphaMs.clone() : new long[n];
        this.sensibilizadaDesdeNs = new long[n];
        this.esperandoVentana = new boolean[n];
        long ahora = System.nanoTime();
        for (int t = 0; t < n; t++) {
            if (sensibilizadasIniciales[t]) sensibilizadaDesdeNs[t] = ahora;
        }
    }

    /** Marca "ahora" en las que pasaron a sensibilizadas, y en la disparada si sigue sensibilizada. */
    public void actualizarTimestamps(boolean[] antes, boolean[] despues, int disparada) {
        long ahora = System.nanoTime();
        for (int t = 0; t < despues.length; t++) {
            if (despues[t] && (!antes[t] || t == disparada)) sensibilizadaDesdeNs[t] = ahora;
        }
    }

    public ResultadoVentana evaluarVentanaTemporal(int t) {
        if (alphaMs[t] <= 0) return ResultadoVentana.NO_TEMPORAL;
        return transcurridoMs(t) < alphaMs[t]
                ? ResultadoVentana.ANTES_DE_VENTANA
                : ResultadoVentana.DENTRO_DE_VENTANA;
    }

    /** Milisegundos que faltan para abrir la ventana (0 si ya se abrió). */
    public long tiempoEspera(int t) {
        return Math.max(0, alphaMs[t] - transcurridoMs(t));
    }

    /** Duerme fuera del mutex. @return true si lo interrumpieron. */
    public boolean dormir(long ms) {
        try {
            Thread.sleep(ms);
            return false;
        } catch (InterruptedException e) {
            return true;
        }
    }

    public void setEsperandoVentana(int t)           {
        esperandoVentana[t] = true;
    }

    public void limpiarEsperandoVentana(int t)       {
        esperandoVentana[t] = false;
    }

    public boolean hayAlguienEsperandoVentana(int t) {
        return esperandoVentana[t];
    }

    private long transcurridoMs(int t) {
        return (System.nanoTime() - sensibilizadaDesdeNs[t]) / 1_000_000L;
    }
}
