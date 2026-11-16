package ar.edu.unc.fcefyn.pcp.tpfinal.monitor;

import java.util.concurrent.Semaphore;

public class Colas {
    private final Semaphore[] semaforos;

    public Colas(int cantidadTransiciones){
        this.semaforos = new Semaphore[cantidadTransiciones];
        for (int i = 0; i < cantidadTransiciones; i++){
            semaforos[i] = new Semaphore(0, true);
        }
    }

    public void esperar(int t){
        try {
            semaforos[t].acquire();
        }
        catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    public void despertar(int t){
        semaforos[t].release();
    }

    public boolean hayEsperando(int t){
        return semaforos[t].hasQueuedThreads();
    }

    public boolean hayEsperando(){
        for (Semaphore semaforo : semaforos) {
            if (semaforo.hasQueuedThreads()) {
                return true;
            }
        }
        return false;
    }

    public boolean[] hilosEsperando(){
        boolean[] resultado = new boolean[semaforos.length];
        for (int i = 0; i < semaforos.length; i++) {
            resultado[i] = semaforos[i].hasQueuedThreads();
        }
        return resultado;
    }

    public void despertarTodos(){
        for (Semaphore semaforo : semaforos) {
            // Abre la cola definitivamente: despierta a los bloqueados y a cualquiera
            // que llegue tarde a esperar() después del fin de la red.
            // Debe invocarse una sola vez (al finalizar la red) para no desbordar los permisos.
            semaforo.release(Integer.MAX_VALUE / 2);
        }
    }
}
