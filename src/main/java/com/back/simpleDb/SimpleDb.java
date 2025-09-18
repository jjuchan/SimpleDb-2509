package com.back.simpleDb;

import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SimpleDb {
    private final String url;
    private final String username;
    private final String password;
    private boolean devMode = false;

    private final ThreadLocal<Connection> threadLocalConnection = new ThreadLocal<>();
    private final Map<Long, Connection> connectionMap = new ConcurrentHashMap<>();
    private final ThreadLocal<Boolean> transactionStatus = new ThreadLocal<>();

    public SimpleDb(String host, String username, String password, String database) {
        this.url = "jdbc:mysql://" + host + ":3306/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        this.username = username;
        this.password = password;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public void run(String sql, Object... params) {
        try (Connection conn = DriverManager.getConnection(url, username, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }

            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Sql genSql() {
        return new Sql(this);
    }

    Connection getConnection() throws SQLException {
        Connection conn = threadLocalConnection.get();
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(url, username, password);
            threadLocalConnection.set(conn);
            connectionMap.put(Thread.currentThread().getId(), conn);

            Boolean inTransaction = transactionStatus.get();
            if (inTransaction != null && inTransaction) {
                conn.setAutoCommit(false);
            }
        }
        return conn;
    }

    public void startTransaction() {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            transactionStatus.set(true);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to start transaction", e);
        }
    }

    public void commit() {
        try {
            Connection conn = threadLocalConnection.get();
            if (conn != null && !conn.isClosed()) {
                conn.commit();
                conn.setAutoCommit(true);
            }
            transactionStatus.remove();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to commit transaction", e);
        }
    }

    public void rollback() {
        try {
            Connection conn = threadLocalConnection.get();
            if (conn != null && !conn.isClosed()) {
                conn.rollback();
                conn.setAutoCommit(true);
            }
            transactionStatus.remove();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to rollback transaction", e);
        }
    }

    public void close() {
        Connection conn = threadLocalConnection.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Failed to close connection: " + e.getMessage());
            } finally {
                threadLocalConnection.remove();
                connectionMap.remove(Thread.currentThread().getId());
                transactionStatus.remove();
            }
        }
    }

    boolean isDevMode() {
        return devMode;
    }
}