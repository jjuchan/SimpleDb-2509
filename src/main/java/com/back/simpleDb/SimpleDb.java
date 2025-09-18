package com.back.simpleDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleDb {
    private final String host;
    private final String username;
    private final String password;
    private final String dbName;
    private boolean devMode = false;

    private final ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<Boolean> transactionMode = ThreadLocal.withInitial(() -> false);

    public SimpleDb(String host, String username, String password, String dbName) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.dbName = dbName;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    Connection getConnection() throws SQLException {
        Connection conn = connectionThreadLocal.get();
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(
                    "jdbc:mysql://" + host + "/" + dbName +
                            "?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8" +
                            "&allowPublicKeyRetrieval=true",
                    username, password
            );
            connectionThreadLocal.set(conn);
        }
        return conn;
    }

    public Sql genSql() {
        return new Sql(this, devMode);
    }

    public void close() {
        try {
            Connection conn = connectionThreadLocal.get();
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
            connectionThreadLocal.remove();
            transactionMode.remove();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void run(String sql) {
        run(sql, new Object[0]);
    }

    public void run(String sql, Object... params) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
                pstmt.execute();
            }

            if (!transactionMode.get()) {
                conn.close();
                connectionThreadLocal.remove();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void startTransaction() {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            transactionMode.set(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            Connection conn = connectionThreadLocal.get();
            if (conn != null && !conn.isClosed()) {
                conn.commit();
                conn.setAutoCommit(true);
            }
            transactionMode.set(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        try {
            Connection conn = connectionThreadLocal.get();
            if (conn != null && !conn.isClosed()) {
                conn.rollback();
                conn.setAutoCommit(true);
            }
            transactionMode.set(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isTransactionMode() {
        return transactionMode.get();
    }
}
