package com.back.simpleDb;

import lombok.Setter;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class SimpleDb {
    private final Connection connection;
    @Setter
    private boolean devMode;
    private final ThreadLocal<Connection> threadLocal = new ThreadLocal<>();

    @SneakyThrows
    public SimpleDb(String host, String username, String password, String db) {
        String url = "jdbc:mysql://" + host + ":3306/" + db;
        this.connection = DriverManager.getConnection(url, username, password);
    }

    @SneakyThrows
    public void run(String sql, Object... params) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        }
    }

    public Sql genSql() {
        return new Sql(this);
    }

    Connection getConnection() {
        return this.connection;
    }
}