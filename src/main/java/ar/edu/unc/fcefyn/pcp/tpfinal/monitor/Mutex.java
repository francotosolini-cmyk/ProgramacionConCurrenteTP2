package ar.edu.unc.fcefyn.pcp.tpfinal.monitor;

import java.util.concurrent.Semaphore;

public class Mutex {
    private final Semaphore semaforo;

    public Mutex() {
        this.semaforo = new Semaphore(1,true);
    }

    public void adquirir(){
        try {
            semaforo.acquire();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void liberar(){
        semaforo.release();
    }

}
