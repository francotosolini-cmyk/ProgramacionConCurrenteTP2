package ar.edu.unc.fcefyn.pcp.tpfinal.registro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoggerTest {

    @Test
    @DisplayName("Logger debe crear el archivo y registrar secuencialmente las transiciones con timestamp")
    void testLoggerEscribeTransiciones(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("logs/test-ejecucion.log");
        Logger logger = new Logger(logPath.toString());

        long antes = System.currentTimeMillis();
        logger.logTransition(0);
        logger.logTransition(1);
        logger.logTransition(2);
        logger.logTransition(3);
        logger.logTransition(9);
        long despues = System.currentTimeMillis();

        logger.close();

        assertTrue(Files.exists(logPath), "El archivo de log debe existir");
        List<String> lineas = Files.readAllLines(logPath);

        assertEquals(5, lineas.size(), "Debe haber 5 lineas registradas");
        assertTrue(lineas.get(0).startsWith("T0 "));
        assertTrue(lineas.get(1).startsWith("T1 "));
        assertTrue(lineas.get(2).startsWith("T2 "));
        assertTrue(lineas.get(3).startsWith("T3 "));
        assertTrue(lineas.get(4).startsWith("T9 "));

        // Verificar que los timestamps sean consistentes
        for (String linea : lineas) {
            String[] partes = linea.split(" ");
            assertEquals(2, partes.length);
            long ts = Long.parseLong(partes[1]);
            assertTrue(ts >= antes && ts <= despues);
        }
    }

    @Test
    @DisplayName("Logger no arroja excepcion si se invoca con constructor por defecto")
    void testLoggerDefaultConstructor() {
        Logger mockLogger = new Logger();
        assertDoesNotThrow(() -> mockLogger.logTransition(4));
        assertDoesNotThrow(mockLogger::close);
        assertNull(mockLogger.getLogFilePath());
    }
}
