package ar.edu.unc.fcefyn.pcp.tpfinal.monitor;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ControlTemporalTest {

    @Test
    void alphaNullOZeroSeEvaluaComoNoTemporal() {
        ControlTemporal controlNull = new ControlTemporal(null, new boolean[]{true, false});
        assertEquals(ControlTemporal.ResultadoVentana.NO_TEMPORAL, controlNull.evaluarVentanaTemporal(0));
        assertEquals(ControlTemporal.ResultadoVentana.NO_TEMPORAL, controlNull.evaluarVentanaTemporal(1));
        assertEquals(0, controlNull.tiempoEspera(0));

        ControlTemporal controlCero = new ControlTemporal(new long[]{0, -5}, new boolean[]{true, true});
        assertEquals(ControlTemporal.ResultadoVentana.NO_TEMPORAL, controlCero.evaluarVentanaTemporal(0));
        assertEquals(ControlTemporal.ResultadoVentana.NO_TEMPORAL, controlCero.evaluarVentanaTemporal(1));
        assertEquals(0, controlCero.tiempoEspera(1));
    }

    @Test
    void transicionTemporalPasaDeAntesDeVentanaADentroDeVentana() {
        ControlTemporal control = new ControlTemporal(new long[]{0, 40}, new boolean[]{true, true});

        assertEquals(ControlTemporal.ResultadoVentana.NO_TEMPORAL, control.evaluarVentanaTemporal(0));
        assertEquals(ControlTemporal.ResultadoVentana.ANTES_DE_VENTANA, control.evaluarVentanaTemporal(1));
        long espera = control.tiempoEspera(1);
        assertTrue(espera > 0 && espera <= 40, "Espera fuera de rango: " + espera);

        assertFalse(control.dormir(50));

        assertEquals(ControlTemporal.ResultadoVentana.DENTRO_DE_VENTANA, control.evaluarVentanaTemporal(1));
        assertEquals(0, control.tiempoEspera(1));
    }

    @Test
    void actualizarTimestampsReiniciaSoloLasNuevasYLaDisparada() {
        ControlTemporal control = new ControlTemporal(
                new long[]{30, 30, 30},
                new boolean[]{true, true, false}
        );

        assertFalse(control.dormir(45));
        assertEquals(ControlTemporal.ResultadoVentana.DENTRO_DE_VENTANA, control.evaluarVentanaTemporal(0));
        assertEquals(ControlTemporal.ResultadoVentana.DENTRO_DE_VENTANA, control.evaluarVentanaTemporal(1));

        // T0 se dispara y sigue sensibilizada; T1 sigue sensibilizada sin dispararse; T2 pasa a sensibilizada
        control.actualizarTimestamps(
                new boolean[]{true, true, false},
                new boolean[]{true, true, true},
                0
        );

        assertEquals(ControlTemporal.ResultadoVentana.ANTES_DE_VENTANA, control.evaluarVentanaTemporal(0),
                "La transición disparada que sigue sensibilizada debe reiniciar su timestamp");
        assertEquals(ControlTemporal.ResultadoVentana.DENTRO_DE_VENTANA, control.evaluarVentanaTemporal(1),
                "Una transición que ya estaba sensibilizada y no se disparó no debe reiniciar su timestamp");
        assertEquals(ControlTemporal.ResultadoVentana.ANTES_DE_VENTANA, control.evaluarVentanaTemporal(2),
                "Una transición recién sensibilizada debe iniciar su ventana temporal");
    }

    @Test
    void flagEsperandoVentanaSeGestionaPorTransicion() {
        ControlTemporal control = new ControlTemporal(new long[]{10, 10, 10}, new boolean[]{false, false, false});

        assertFalse(control.hayAlguienEsperandoVentana(0));
        assertFalse(control.hayAlguienEsperandoVentana(1));

        control.setEsperandoVentana(1);
        assertFalse(control.hayAlguienEsperandoVentana(0));
        assertTrue(control.hayAlguienEsperandoVentana(1));

        control.limpiarEsperandoVentana(1);
        assertFalse(control.hayAlguienEsperandoVentana(1));
    }

    @Test
    void dormirDevuelveTrueSiSeInterrumpeElHilo() {
        ControlTemporal control = new ControlTemporal(new long[]{5000}, new boolean[]{true});
        AtomicBoolean interrumpido = new AtomicBoolean(false);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            Thread hilo = new Thread(() -> interrumpido.set(control.dormir(5000)));
            hilo.start();
            Thread.sleep(30);
            hilo.interrupt();
            hilo.join();
        });

        assertTrue(interrumpido.get());
    }
}
