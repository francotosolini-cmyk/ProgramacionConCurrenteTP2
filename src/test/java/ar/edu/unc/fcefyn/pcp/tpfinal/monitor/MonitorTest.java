package ar.edu.unc.fcefyn.pcp.tpfinal.monitor;

import ar.edu.unc.fcefyn.pcp.tpfinal.hilos.AdmisionWorker;
import ar.edu.unc.fcefyn.pcp.tpfinal.hilos.AltoRiesgoWorker;
import ar.edu.unc.fcefyn.pcp.tpfinal.hilos.SalidaWorker;
import ar.edu.unc.fcefyn.pcp.tpfinal.hilos.TarjetaWorker;
import ar.edu.unc.fcefyn.pcp.tpfinal.hilos.TransferenciaWorker;
import ar.edu.unc.fcefyn.pcp.tpfinal.politica.Politica;
import ar.edu.unc.fcefyn.pcp.tpfinal.politica.PriorityPolicy;
import ar.edu.unc.fcefyn.pcp.tpfinal.red.ConfigPagos;
import ar.edu.unc.fcefyn.pcp.tpfinal.red.RedPetri;
import ar.edu.unc.fcefyn.pcp.tpfinal.registro.Logger;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MonitorTest {

    private static final int TARGET = 200;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /* ---------- Dobles de prueba ---------- */

    /** Política de prueba que elige la primera candidata disponible. */
    static class PoliticaPrimera implements Politica {
        @Override
        public int elegir(boolean[] c) {
            for (int i = 0; i < c.length; i++) if (c[i]) return i;
            throw new IllegalStateException("elegir() sin candidatas");
        }
    }

    /** Política que elige una candidata al azar. */
    static class PoliticaAleatoria extends PoliticaPrimera {
        @Override public int elegir(boolean[] c) {
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < c.length; i++) if (c[i]) idx.add(i);
            if (idx.isEmpty()) throw new IllegalStateException("elegir() sin candidatas");
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

    static class PoliticaConRegistro extends PoliticaPrimera {
        private final LoggerExclusion log;
        private int disparosUltimaConsulta = 0;
        final AtomicBoolean consultaSinDisparo = new AtomicBoolean(false);

        PoliticaConRegistro(LoggerExclusion log) {
            this.log = log;
        }

        @Override
        public int elegir(boolean[] candidatas) {
            int disparosActuales = log.disparos.get();

            if (disparosActuales <= disparosUltimaConsulta) {
                consultaSinDisparo.set(true);
            }

            disparosUltimaConsulta = disparosActuales;
            return super.elegir(candidatas);
        }
    }

    /* ---------- Utilidades ---------- */

    private static void esperarEnCola(Thread hilo) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (hilo.getState() != Thread.State.WAITING
                && hilo.isAlive() && System.nanoTime() < limite) {
            Thread.sleep(1);
        }

        assertEquals(Thread.State.WAITING, hilo.getState(),
                "El hilo no llegó a su cola: " + hilo.getName());
    }

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

    @Test
    void monitorConTiemposAlfaYPriorityPolicyCompleta200Invariantes() throws InterruptedException {
        long[] alphaCorto = {0, 0, 3, 3, 0, 4, 0, 3, 3, 0};
        ConfigPagos config = new ConfigPagos("resultados/logs/test-ignorado.log", alphaCorto);
        RedPetri red = new RedPetri(config);
        LoggerExclusion log = new LoggerExclusion();
        Monitor m = new Monitor(red, new PriorityPolicy(), log, config.getAlphaTimes());

        List<Thread> hilos = new ArrayList<>();
        for (int i = 0; i < 3; i++) hilos.add(new Thread(new AdmisionWorker(m), "Admision-" + i));
        hilos.add(new Thread(new TarjetaWorker(m), "Tarjeta-0"));
        hilos.add(new Thread(new AltoRiesgoWorker(m), "AltoRiesgo-0"));
        hilos.add(new Thread(new TransferenciaWorker(m), "Transferencia-0"));
        for (int i = 0; i < 3; i++) hilos.add(new Thread(new SalidaWorker(m), "Salida-" + i));

        ejecutar(hilos, red);
        verificarFinal(red, log);
    }

    @Test
    void monitorRespetaVentanaTemporalAntesDeDisparar() {
        long[] alpha = {0, 0, 40, 0, 0, 0, 0, 0, 0, 0};
        RedPetri red = new RedPetri(new ConfigPagos());
        LoggerExclusion log = new LoggerExclusion();
        Monitor m = new Monitor(red, new PoliticaPrimera(), log, alpha);

        assertTrue(m.fireTransition(ConfigPagos.T0));
        long antesDeSensibilizarT2 = System.nanoTime();
        assertTrue(m.fireTransition(ConfigPagos.T1));
        assertTrue(m.fireTransition(ConfigPagos.T2));
        long transcurridoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - antesDeSensibilizarT2);

        assertTrue(transcurridoMs >= 40, "T2 debió esperar al menos 40 ms, esperó " + transcurridoMs + " ms");
    }

    @Test
    void dosHilosEnMismaTransicionTemporalNoDuplicanEsperaDeVentana() throws InterruptedException {
        long[] alpha = {60, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        RedPetri red = new RedPetri(new ConfigPagos());
        LoggerExclusion log = new LoggerExclusion();
        Monitor m = new Monitor(red, new PoliticaPrimera(), log, alpha);

        CountDownLatch arranque = new CountDownLatch(1);
        AtomicBoolean r1 = new AtomicBoolean(false);
        AtomicBoolean r2 = new AtomicBoolean(false);

        Thread h1 = new Thread(() -> {
            try {
                arranque.await();
                r1.set(m.fireTransition(ConfigPagos.T0));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        Thread h2 = new Thread(() -> {
            try {
                arranque.await();
                r2.set(m.fireTransition(ConfigPagos.T0));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });

        long inicioNs = System.nanoTime();
        h1.start();
        h2.start();
        arranque.countDown();
        h1.join(3000);
        h2.join(3000);
        long totalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNs);

        assertFalse(h1.isAlive());
        assertFalse(h2.isAlive());
        assertTrue(r1.get());
        assertTrue(r2.get());
        assertEquals(2, red.getFireCount(ConfigPagos.T0));
        assertTrue(totalMs >= 120, "Dos disparos secuenciales con α=60 ms debían demorar >= 120 ms, demoró " + totalMs + " ms");
    }

    @Test
    void hiloInterrumpidoDuranteEsperaDeVentanaSaleConFalseYLimpiaFlag() {
        long[] alpha = {150, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        RedPetri red = new RedPetri(new ConfigPagos());
        LoggerExclusion log = new LoggerExclusion();
        Monitor m = new Monitor(red, new PoliticaPrimera(), log, alpha);

        AtomicBoolean resultado = new AtomicBoolean(true);
        AtomicBoolean flagInterrumpido = new AtomicBoolean(false);

        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            Thread hilo = new Thread(() -> {
                resultado.set(m.fireTransition(ConfigPagos.T0));
                flagInterrumpido.set(Thread.currentThread().isInterrupted());
            });
            hilo.start();
            Thread.sleep(25);
            hilo.interrupt();
            hilo.join();

            assertFalse(resultado.get());
            assertTrue(flagInterrumpido.get());
            assertEquals(0, red.getFireCount(ConfigPagos.T0));

            // Verifica que el mutex fue liberado y esperandoVentana[T0] fue limpiado
            assertTrue(m.fireTransition(ConfigPagos.T0));
            assertEquals(1, red.getFireCount(ConfigPagos.T0));
        });
    }
    @Test
    void noConsultaPoliticaAlEsperarLaVentanaTemporal()
            throws InterruptedException {

        long[] alpha = new long[ConfigPagos.TRANSICIONES_COUNT];
        alpha[ConfigPagos.T4] = 1000;

        RedPetri red = new RedPetri(new ConfigPagos());
        LoggerExclusion log = new LoggerExclusion();
        PoliticaConRegistro politica = new PoliticaConRegistro(log);
        Monitor m = new Monitor(red, politica, log, alpha);

        Thread riesgo = new Thread(() -> {
            m.fireTransition(ConfigPagos.T4);
            m.fireTransition(ConfigPagos.T5);
        }, "Riesgo-prueba");

        Thread transferencia = new Thread(() -> {
            m.fireTransition(ConfigPagos.T6);
            m.fireTransition(ConfigPagos.T7);
            m.fireTransition(ConfigPagos.T8);
        }, "Transferencia-prueba");

        riesgo.setDaemon(true);
        transferencia.setDaemon(true);

        riesgo.start();
        esperarEnCola(riesgo);

        transferencia.start();
        esperarEnCola(transferencia);

        assertEquals(0, log.disparos.get());
        assertFalse(politica.consultaSinDisparo.get());

        assertTrue(m.fireTransition(ConfigPagos.T0));
        assertTrue(m.fireTransition(ConfigPagos.T0));

        riesgo.join(5000);
        transferencia.join(5000);

        assertFalse(riesgo.isAlive(), "El hilo de alto riesgo no terminó");
        assertFalse(transferencia.isAlive(),
                "El hilo de transferencia no terminó");

        assertEquals(7, log.disparos.get());

        assertFalse(politica.consultaSinDisparo.get(),
                "Se consultó a la política sin un nuevo disparo exitoso");
    }
}
