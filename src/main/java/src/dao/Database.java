package src.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private Database() {}

    public static Connection connect(String dbFilePath) throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            // WAL lets concurrent readers proceed without blocking on a writer; busy_timeout
            // makes a writer that arrives while another transaction is committing wait and
            // retry instead of failing immediately with SQLITE_BUSY.
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        initSchema(connection);
        return connection;
    }

    private static void initSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String ddl : readSchema().split(";")) {
                String trimmed = ddl.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        }
    }

    private static String readSchema() {
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql not found on classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
