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
java -jar target/tpfinal.jar
```

Windows:

```bat
mvnw.cmd package
java -jar target\tpfinal.jar
```

Para correr solo los tests: `./mvnw test` (o `mvnw.cmd test` en Windows).

Los comandos deben ejecutarse desde la raíz del repositorio, ya que el programa lee la
configuración de `config/` y escribe los logs en `resultados/logs/`.
