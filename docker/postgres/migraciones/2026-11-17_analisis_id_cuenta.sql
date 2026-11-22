-- Aplicar UNA vez en producción (RDS descortezadores-db), en transacción:
--   psql "$URL" -v ON_ERROR_STOP=1 -1 -f 2026-11-17_analisis_id_cuenta.sql
-- Idempotente (IF NOT EXISTS): volver a aplicarla no hace nada. Añadir una columna nula sin DEFAULT
-- solo toca el catálogo (lock breve sobre analisis, sin reescribir la tabla); no modifica datos.
-- analisis no guardaba quién lo subió: la sincronización recibe la cuenta autenticada pero solo la
-- dejaba en arbol / zona_follaje al crearlos. Con id_cuenta la web puede mostrar el autor de cada
-- foto y cuántas personas le dan seguimiento a un mismo árbol o zona.
-- Los análisis existentes quedan con id_cuenta NULL (autor desconocido); no se rellenan con el dueño
-- del árbol porque no hay forma de saber quién subió cada foto. ON DELETE SET NULL: borrar una cuenta
-- no borra sus análisis.
-- Orden: aplicar esta migración ANTES de desplegar el TT_BKN que la usa. Ese TT_BKN lee y escribe
-- analisis.id_cuenta en TODA operación sobre la entidad Analisis: sin la columna fallan la sincronización
-- (/sync responde 200 pero con totalSincronizados = 0 y "column id_cuenta does not exist" en errores) y todas
-- las lecturas de análisis (historial, GET /api/v1/analisis, /arboles/{id}/analisis, estadísticas y
-- reportes responden 500). b-sheet funciona con o sin ella (si falta, responde autor = null); detecta la
-- columna una vez por arranque, así que el autor aparece a partir de su siguiente arranque en frío o
-- redespliegue.
-- Rollback, en este orden:
--   1. Volver TT_BKN a la versión anterior (la que no conoce id_cuenta).
--   2. En la BD:
--        DROP INDEX IF EXISTS idx_analisis_cuenta;
--        ALTER TABLE analisis DROP COLUMN IF EXISTS id_cuenta;
--   3. Reiniciar o redesplegar b-sheet: recuerda que la columna existía y respondería 500 en los análisis
--      hasta su siguiente arranque en frío.
ALTER TABLE analisis
    ADD COLUMN IF NOT EXISTS id_cuenta UUID REFERENCES cuenta (id_cuenta) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_analisis_cuenta ON analisis (id_cuenta);
