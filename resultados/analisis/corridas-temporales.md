## Corridas con tiempos activados

- **Configuración A:** T2, T3, T7 y T8 en 50 ms. T5 en 120 ms.
- **Configuración B:** T2, T3, T7 y T8 en 100 ms. T5 en 120 ms.

Las ocho corridas completaron 200 ciclos, validaron los invariantes del registro y terminaron con los nueve hilos cerrados.

| Config. | Política | Tarjeta | Alto riesgo | Transferencia | Duración | Registro |
|---|---|---:|---:|---:|---:|---|
| A | `random` | 100 | 0 | 100 | 12,581 s | `random-piloto-01.log` |
| A | `priority 0.8` | 14 | 172 | 14 | 23,348 s | `priority-0.8-piloto-01.log` |
| A | `random` | 67 | 66 | 67 | 16,698 s | `ejecucion_random_2026-11-17_16-15-15-467.log` |
| A | `priority 0.8` | 20 | 160 | 20 | 22,622 s | `ejecucion_priority-0.8_2026-11-17_16-15-32-744.log` |
| B | `random` | 67 | 66 | 67 | 23,044 s | `ejecucion_random_2026-11-17_16-18-14-282.log` |
| B | `priority 0.8` | 18 | 164 | 18 | 24,604 s | `ejecucion_priority-0.8_2026-11-17_16-18-38-172.log` |
| B | `random` | 83 | 33 | 84 | 22,581 s | `ejecucion_random_2026-11-17_16-25-21-895.log` |
| B | `priority 0.8` | 17 | 166 | 17 | 24,594 s | `ejecucion_priority-0.8_2026-11-17_16-25-45-045.log` |

En las cuatro corridas de la configuración B, ambas políticas completaron 200 ciclos, validaron el registro, cerraron los nueve hilos y quedaron dentro de los 20–40 s. `random` tardó en promedio 22,81 s y `priority 0.8`, 24,60 s. Sumando las dos corridas de cada política, alto riesgo pasó de 99 de 400 pagos (24,75 %) con `random` a 330 de 400 (82,50 %) con `priority 0.8`. Tarjeta y transferencia también procesaron pagos en todas las corridas de B.

## Estimación analítica y comparación

Tarjeta ocupa el gateway durante T2 y T3. Transferencia ocupa el antifraude durante T7 y T8. Alto riesgo ocupa ambos recursos durante T5.

Para cada corrida calculamos:

- Tiempo mínimo del gateway = pagos con tarjeta × (T2 + T3) + pagos de alto riesgo × T5.
- Tiempo mínimo del antifraude = transferencias × (T7 + T8) + pagos de alto riesgo × T5.

El mínimo estimado de la corrida es el mayor de esos dos valores. Tomamos el mayor porque tarjeta y transferencia pueden usar sus recursos en paralelo.

Por ejemplo, en la primera corrida de B con `random`, hubo 67 pagos con tarjeta, 66 de alto riesgo y 67 transferencias. Cada recurso acumuló `67 × 0,200 s + 66 × 0,120 s = 21,32 s` de ocupación mínima. La duración medida fue de 23,044 s, unos 1,72 s por encima del mínimo.

Calculamos ese mínimo para cada corrida y promediamos los dos resultados de cada combinación:

| Configuración | Política | Mínimo estimado medio | Tiempo medido medio | Diferencia |
|---|---|---:|---:|---:|
| A | `random` | 12,31 s | 14,64 s | 2,33 s |
| A | `priority 0.8` | 21,62 s | 22,99 s | 1,37 s |
| B | `random` | 21,04 s | 22,81 s | 1,77 s |
| B | `priority 0.8` | 23,30 s | 24,60 s | 1,30 s |

El mínimo estimado se calcula con los conteos observados y los tiempos configurados. La duración medida también incluye la coordinación de los hilos, el registro de disparos y los intervalos en que los recursos quedan libres.

Al pasar de A a B se duplicaron las esperas de tarjeta y transferencia. El tiempo medio subió más con `random`, donde esos dos flujos procesaron más pagos. En las cuatro corridas de B, ambas políticas quedaron dentro del rango de 20–40 s.