package io.github.pobsteta.surculus.gpkg

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.DriverManager

/** Le pilote SQLite JVM du module `:gpkg` s'ouvre et répond, sans appareil (ADR 0004). */
class SqliteJdbcTest {

    @Test
    fun `une base sqlite en memoire s ouvre et repond`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { cnx ->
            cnx.createStatement().use { st ->
                st.executeUpdate("CREATE TABLE t (id INTEGER PRIMARY KEY, nom TEXT NOT NULL)")
                st.executeUpdate("INSERT INTO t (nom) VALUES ('placette')")
                st.executeQuery("SELECT count(*), sqlite_version() FROM t").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt(1))
                    assertTrue(rs.getString(2).startsWith("3."))
                }
            }
        }
    }
}
