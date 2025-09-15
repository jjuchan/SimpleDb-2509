package com.back.simpleDb;

import lombok.SneakyThrows;

import java.sql.*;

public class SimpleDb {

    private Connection connection;
    private boolean devMode;


    @SneakyThrows
    public SimpleDb(String host, String username, String password, String dbName) {
        this.connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":3306/" + dbName, username, password
        );
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public Connection getConnection() {
        return connection;
    }

    @SneakyThrows
    public void run(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }
            stmt.execute();
        }
    }

    public Sql genSql() {

        return new Sql(this);
    }

    @SneakyThrows
    public long insert(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }
    @SneakyThrows
    public int update(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        }

    }
}
