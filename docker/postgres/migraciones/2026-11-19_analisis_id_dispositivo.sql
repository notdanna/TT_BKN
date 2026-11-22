-- Migración 2026-11-19: registrar dispositivo de captura en cada análisis individual
ALTER TABLE analisis
    ADD COLUMN IF NOT EXISTS id_dispositivo UUID REFERENCES dispositivo(id_dispositivo) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_analisis_dispositivo ON analisis(id_dispositivo);

-- Backfill opcional de registros históricos sin dispositivo hacia el Pixel 7 Pro existente
UPDATE analisis
SET id_dispositivo = (
    SELECT id_dispositivo FROM dispositivo
    WHERE identificador_unico <> 'android-test-123'
      AND LOWER(COALESCE(nombre_dispositivo, '')) LIKE '%pixel 7 pro%'
    ORDER BY fecha_registro ASC NULLS LAST
    LIMIT 1
)
WHERE id_dispositivo IS NULL;
