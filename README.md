# ProgramacionConCurrenteTP2
TRABAJO PRACTICO FINAL PROGRAMACION PARALELA Y CONCURRENTE

## Requisitos

- JDK 21 o superior (`java -version` para verificar).

No hace falta instalar Maven ni usar un IDE en particular: el proyecto incluye el Maven Wrapper
(`mvnw` / `mvnw.cmd`), que descarga automáticamente la versión correcta de Maven la primera vez.

## Compilar y ejecutar

Linux / macOS:

```bash
./mvnw package
java -jar target/tpfinal.jar random
java -jar target/tpfinal.jar priority 0.8
```

Windows:

```bat
.\mvnw.cmd package
java -jar .\target\tpfinal.jar random
java -jar .\target\tpfinal.jar priority 0.8
```

Sin argumentos se utiliza la política `random`. Con `priority` se prioriza el flujo de alto riesgo. La probabilidad es opcional, debe estar entre 0 y 1 y su valor predeterminado es `0.8`.

Para correr solo los tests: `./mvnw test` (o `.\mvnw.cmd test` en Windows).

Los comandos deben ejecutarse desde la raíz del repositorio. La configuración de la red
y los tiempos se define en `ConfigPagos.java`, y los logs se escriben en `resultados/logs/`.

## Tiempos de ejecución

La configuración predeterminada utiliza 100 ms para T2, T3, T7 y T8, y 120 ms para T5.
Cada ejecución completa 200 ciclos y termina los nueve hilos.

Las cuatro corridas documentadas con esta configuración duraron entre 20 y 40 segundos.
Las mediciones y el análisis están en
[resultados/analisis/corridas-temporales.md](resultados/analisis/corridas-temporales.md).

Después de modificar el código o los tiempos, volver a ejecutar `package` para actualizar el JAR.

## Logs y resultados

Cada corrida genera un archivo en `resultados/logs/`, cuyo nombre incluye la política,
la probabilidad cuando corresponde, y la fecha y hora.

Al finalizar, el programa valida el registro y muestra la cantidad y el porcentaje de pagos
por flujo, junto con la duración total.

Los logs están ignorados por Git. Conservar los archivos utilizados como evidencia del análisis.
