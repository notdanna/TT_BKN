package com.descortezadores.infrastructure.persistence;

import io.micronaut.context.event.StartupEvent;
import io.micronaut.data.connection.annotation.Connectable;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asegura de forma idempotente que las columnas e índices añadidos después de la
 * creación inicial de la base de datos existan en PostgreSQL (por ejemplo
 * {@code analisis.id_cuenta} para atribución de autor y {@code analisis.id_dispositivo}
 * para identificar el teléfono de captura) y rellena registros previos cuando aplica.
 */
@Singleton
public class MigracionEsquemaStartup {

    private static final Logger LOG = LoggerFactory.getLogger(MigracionEsquemaStartup.class);

    private final DataSource dataSource;

    public MigracionEsquemaStartup(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Ejecuta la migración idempotente de esquema al iniciar el contexto Micronaut.
     */
    @EventListener
    @Connectable
    public void onStartup(StartupEvent event) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String dbName = metaData.getDatabaseProductName();
            if (dbName == null || !dbName.toLowerCase().contains("postgresql")) {
                LOG.debug("Omitiendo migración PostgreSQL en base de datos '{}'", dbName);
                return;
            }

            try (Statement st = conn.createStatement()) {
                st.execute(
                    "ALTER TABLE analisis ADD COLUMN IF NOT EXISTS id_cuenta UUID REFERENCES cuenta(id_cuenta) ON DELETE SET NULL"
                );
                st.execute(
                    "CREATE INDEX IF NOT EXISTS idx_analisis_cuenta ON analisis(id_cuenta)"
                );
                int backfillArbol = st.executeUpdate(
                    "UPDATE analisis a SET id_cuenta = ar.id_cuenta FROM arbol ar " +
                    "WHERE a.id_arbol = ar.id_arbol AND a.id_cuenta IS NULL AND ar.id_cuenta IS NOT NULL"
                );
                int backfillZona = st.executeUpdate(
                    "UPDATE analisis a SET id_cuenta = z.id_cuenta FROM zona_follaje z " +
                    "WHERE a.id_zona = z.id_zona AND a.id_cuenta IS NULL AND z.id_cuenta IS NOT NULL"
                );

                st.execute(
                    "ALTER TABLE analisis ADD COLUMN IF NOT EXISTS id_dispositivo UUID REFERENCES dispositivo(id_dispositivo) ON DELETE SET NULL"
                );
                st.execute(
                    "CREATE INDEX IF NOT EXISTS idx_analisis_dispositivo ON analisis(id_dispositivo)"
                );

                UUID pixel7ProId = buscarPixel7Pro(conn);
                int dispAnalisis = 0;
                int dispArbol = 0;
                int dispZona = 0;
                if (pixel7ProId != null) {
                    dispAnalisis = rellenarDispositivoNulo(conn, "analisis", pixel7ProId);
                    dispArbol = rellenarDispositivoNulo(conn, "arbol", pixel7ProId);
                    dispZona = rellenarDispositivoNulo(conn, "zona_follaje", pixel7ProId);
                }

                LOG.info(
                    "Migración PostgreSQL verificada (id_cuenta arbol={}, zona={}; id_dispositivo={}, analisis={}, arbol={}, zona={})",
                    backfillArbol,
                    backfillZona,
                    pixel7ProId,
                    dispAnalisis,
                    dispArbol,
                    dispZona
                );
            }
        } catch (SQLException ex) {
            LOG.warn("No se pudo ejecutar migración startup PostgreSQL: {}", ex.getMessage());
        }
    }

    private static UUID buscarPixel7Pro(Connection conn) throws SQLException {
        String sql = "SELECT id_dispositivo FROM dispositivo " +
            "WHERE identificador_unico <> 'android-test-123' " +
            "AND LOWER(COALESCE(nombre_dispositivo, '')) LIKE '%pixel 7 pro%' " +
            "ORDER BY fecha_registro ASC NULLS LAST LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getObject(1, UUID.class) : null;
        }
    }

    private static int rellenarDispositivoNulo(Connection conn, String tabla, UUID idDispositivo) throws SQLException {
        String sql = "UPDATE " + tabla + " SET id_dispositivo = ? WHERE id_dispositivo IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, idDispositivo);
            return ps.executeUpdate();
        }
    }
}
