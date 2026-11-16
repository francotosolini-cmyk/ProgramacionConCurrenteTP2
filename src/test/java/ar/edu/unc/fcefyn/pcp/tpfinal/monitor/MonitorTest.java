package ar.edu.unc.fcefyn.pcp.tpfinal.monitor;

import ar.edu.unc.fcefyn.pcp.tpfinal.politica.Politica;
import ar.edu.unc.fcefyn.pcp.tpfinal.red.ConfigPagos;
import ar.edu.unc.fcefyn.pcp.tpfinal.red.RedPetri;
import ar.edu.unc.fcefyn.pcp.tpfinal.registro.Logger;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MonitorTest {

    private static final int TARGET = 200;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /* ---------- Dobles de prueba ---------- */

    /** Política sin restricciones: permite todo lo sensibilizado y elige la primera candidata. */
    static class PoliticaPrimera implements Politica {
        public boolean[] permitidas(boolean[] s) { return s.clone(); }
        public int elejir(boolean[] c) {
            for (int i = 0; i < c.length; i++) if (c[i]) return i;
            throw new IllegalStateException("elejir() sin candidatas");
        }
    }

    /** Política que elige una candidata al azar. */
    static class PoliticaAleatoria extends PoliticaPrimera {
        @Override public int elejir(boolean[] c) {
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < c.length; i++) if (c[i]) idx.add(i);
            if (idx.isEmpty()) throw new IllegalStateException("elejir() sin candidatas");
            return idx.get(ThreadLocalRandom.current().nextInt(idx.size()));
        }
    }

    /** Logger que detecta si dos hilos están dentro de la sección crítica a la vez. */
    static class LoggerExclusion extends Logger {
        final AtomicInteger dentro = new AtomicInteger();
        final AtomicInteger violaciones = new AtomicInteger();
        final AtomicInteger disparos = new AtomicInteger();
        @Override public void logTransition(int t) {
            if (dentro.incrementAndGet() != 1) violaciones.incrementAndGet();
            disparos.incrementAndGet();
            Thread.yield();
            dentro.decrementAndGet();
        }
    }

    /* ---------- Utilidades ---------- */

    /** Lanza los hilos, espera a que terminen y falla (con volcado de estado) si se cuelgan. */
    private static void ejecutar(List<Thread> hilos, RedPetri red) throws InterruptedException {
        hilos.forEach(Thread::start);
        long limite = System.nanoTime() + TIMEOUT.toNanos();
        for (Thread h : hilos) {
            h.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(limite - System.nanoTime())));
        }
        List<String> vivos = new ArrayList<>();
        for (Thread h : hilos) if (h.isAlive()) vivos.add(h.getName());
        if (!vivos.isEmpty()) {
            hilos.forEach(Thread::interrupt);
            fail("Deadlock: hilos bloqueados " + vivos
                    + " | marcado=" + java.util.Arrays.toString(red.getMarcadoActual())
                    + " | disparos=" + java.util.Arrays.toString(red.getFireCount()));
        }
    }

    /** Hilo que dispara siempre la misma transición hasta que el monitor devuelve false. */
    private static Thread hiloTransicion(Monitor m, int t, String nombre) {
        return new Thread(() -> { while (m.fireTransition(t)) { } }, nombre);
    }

    /** Hilo que recorre una secuencia (camino de un invariante de transición) en bucle. */
    private static Thread hiloSecuencia(Monitor m, int[] seq, String nombre) {
        return new Thread(() -> {
            while (true) for (int t : seq) if (!m.fireTransition(t)) return;
        }, nombre);
    }

    private static void verificarFinal(RedPetri red, LoggerExclusion log) {
        int[] fc = red.getFireCount();
        assertTrue(red.isFinished(), "La red debería haber terminado");
        assertEquals(TARGET, fc[ConfigPagos.T0], "Admisiones");
        assertEquals(TARGET, fc[ConfigPagos.T9], "Salidas");
        assertEquals(fc[ConfigPagos.T1], fc[ConfigPagos.T2]);
        assertEquals(fc[ConfigPagos.T2], fc[ConfigPagos.T3]);
        assertEquals(fc[ConfigPagos.T4], fc[ConfigPagos.T5]);
        assertEquals(fc[ConfigPagos.T6], fc[ConfigPagos.T7]);
        assertEquals(fc[ConfigPagos.T7], fc[ConfigPagos.T8]);
        assertEquals(java.util.Arrays.stream(fc).sum(), log.disparos.get(), "Cada disparo se loguea una vez");
        assertEquals(0, log.violaciones.get(), "Exclusión mutua violada");
    }

    /* ---------- Casos ---------- */

    @RepeatedTest(30)
    void unHiloPorTransicion() throws InterruptedException {
        RedPetri red = new RedPetri(new ConfigPagos());
        LoggerExclusion log = new LoggerExclusion();
        Monitor m = new Monitor(red, new PoliticaPrimera(), log);

        List<Thread> hilos = new ArrayList<>();
        for (int t = 0; t < ConfigPagos.TRANSICIONES_COUNT; t++) hilos.add(hiloTransicion(m, t, "T" + t));
        ejecutar(hilos, red);
        verificarFinal(red, log);
    }

    @RepeatedTest(30)
    void variosHilosPorTransicionPoliticaAleatoria() throws InterruptedException {
        RedPetri red = new RedPetri(new ConfigPagos());
        LoggerExclusion log = new LoggerExclusion();
        Monitor m = new Monitor(red, new PoliticaAleatoria(), log);

        List<Thread> hilos = new ArrayList<>();
        for (int t = 0; t < ConfigPagos.TRANSICIONES_COUNT; t++)
            for (int k = 0; k < 3; k++) hilos.add(hiloTransicion(m, t, "T" + t + "-" + k));
        ejecutar(hilos, red);
        verificarFinal(red, log);
    }

    @RepeatedTest(30)
    void hilosPorCaminoDeInvariante() throws InterruptedException {
        RedPetri red = new RedPetri(new ConfigPagos());
        LoggerExclusion log = new LoggerExclusion();
        Monitor m = new Monitor(red, new PoliticaAleatoria(), log);

        int[][] caminos = {
                {ConfigPagos.T0}, {ConfigPagos.T9},
                {ConfigPagos.T1, ConfigPagos.T2, ConfigPagos.T3},
                {ConfigPagos.T4, ConfigPagos.T5},
                {ConfigPagos.T6, ConfigPagos.T7, ConfigPagos.T8},
        };
        List<Thread> hilos = new ArrayList<>();
        for (int i = 0; i < caminos.length; i++)
            for (int k = 0; k < 2; k++) hilos.add(hiloSecuencia(m, caminos[i], "camino" + i + "-" + k));
        ejecutar(hilos, red);
        verificarFinal(red, log);
    }

    @Test
    void llamadasDespuesDelFinDevuelvenFalseSinBloquearse() throws InterruptedException {
        RedPetri red = new RedPetri(new ConfigPagos());
        LoggerExclusion log = new LoggerExclusion();
        Monitor m = new Monitor(red, new PoliticaPrimera(), log);

        List<Thread> hilos = new ArrayList<>();
        for (int t = 0; t < ConfigPagos.TRANSICIONES_COUNT; t++) hilos.add(hiloTransicion(m, t, "T" + t));
        ejecutar(hilos, red);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            for (int t = 0; t < ConfigPagos.TRANSICIONES_COUNT; t++) assertFalse(m.fireTransition(t));
        });
    }

    @Test
    void mutexGarantizaExclusionMutua() throws InterruptedException {
        Mutex mutex = new Mutex();
        AtomicInteger dentro = new AtomicInteger();
        AtomicInteger violaciones = new AtomicInteger();
        int[] contador = {0};

        List<Thread> hilos = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            hilos.add(new Thread(() -> {
                for (int j = 0; j < 10_000; j++) {
                    mutex.adquirir();
                    if (dentro.incrementAndGet() != 1) violaciones.incrementAndGet();
                    contador[0]++;
                    dentro.decrementAndGet();
                    mutex.liberar();
                }
            }));
        }
        hilos.forEach(Thread::start);
        for (Thread h : hilos) h.join();
        assertEquals(0, violaciones.get());
        assertEquals(80_000, contador[0]);
    }

    @Test
    void colasDespertarTodosLiberaBloqueadosYRezagados() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            Colas colas = new Colas(3);
            Thread bloqueado = new Thread(() -> colas.esperar(1));
            bloqueado.start();
            while (!colas.hayEsperando(1)) Thread.yield();
            assertArrayEquals(new boolean[]{false, true, false}, colas.hilosEsperando());

            colas.despertarTodos();
            bloqueado.join();
            colas.esperar(0); // rezagado: llega después del cierre y no debe bloquearse
            colas.esperar(2);
            assertFalse(colas.hayEsperando());
        });
    }
}
