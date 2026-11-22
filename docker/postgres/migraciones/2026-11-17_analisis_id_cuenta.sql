-- Agrega id_cuenta en analisis para registrar el autor de cada captura (usado por b-sheet/f-sheet).
ALTER TABLE analisis
    ADD COLUMN IF NOT EXISTS id_cuenta UUID REFERENCES cuenta(id_cuenta) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_analisis_cuenta ON analisis(id_cuenta);
