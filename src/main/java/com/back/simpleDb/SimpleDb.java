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
    private final String dbUrl;

    // thread별 connection
    // 요구조건: simpleDb 객체는 멀티 쓰레드 환경에서 공유되어도 문제가 없어야 한다                                                                                              
    //           simpleDb 객체는 DB Connection 객체를 여러개 가지고 있어야 한다                                                                                                     
    //           쓰레드 당 1개       
    private final ThreadLocal<Connection> threadConnection = new ThreadLocal<>();

    // thread별 트랜잭션 상태관리용
    private final ThreadLocal<Boolean> openTransaction = new ThreadLocal<>();

    // 개발 모드 플래그
    private boolean devMode = false;

    // 요구조건: new SimpleDb("localhost", "root", "root123414", "simpleDb__test")
    public SimpleDb(String host, String username, String password, String dbName) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.dbName = dbName;
        this.dbUrl = "jdbc:mysql://" + host + ":3306/" + dbName;
    }

    // 개발 모드 설정
    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    // 현재 thread connection 가져오기, 없으면 생성
    private Connection getConnection() throws SQLException {
        Connection conn = threadConnection.get();
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(dbUrl, username, password);
            threadConnection.set(conn);

            // 트랜잭션 상태가 아니면 AutoCommit true
            if (!isOpenTransaction()) {
                conn.setAutoCommit(true);
            }
        }
        return conn;
    }

    // thread 트랜잭션 상태 확인(트랜잭션 진행 여부 확인)
    private boolean isOpenTransaction() {
        Boolean inTx = openTransaction.get();
        return inTx != null && inTx;
    }

    // Sql 객체 생성
    public Sql genSql() {
        return new Sql(this);
    }

    // SQL 실행용
    // 요구조건: simpleDb.run("DROP TABLE IF EXISTS article")
    public void run(String sql, Object... params) {
        try {
            Connection conn = getConnection();

            if (devMode) {
                System.out.println("SQL: " + sql);
                if (params.length > 0) {
                    System.out.println("Parameters: " + java.util.Arrays.toString(params));
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                setParameters(pstmt, params);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("SQL 실행 중 오류 발생", e);
        }
    }

    // 파라미터 바인딩
    private void setParameters(PreparedStatement pstmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            pstmt.setObject(i + 1, params[i]);
        }
    }

    // 트랜잭션 시작
    // 요구조건: simpleDb.startTransaction()
    public void startTransaction() {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            openTransaction.set(true);

            if (devMode) {
                System.out.println("Transaction started");
            }
        } catch (SQLException e) {
            throw new RuntimeException("트랜잭션 시작 중 오류 발생", e);
        }
    }

    // 트랜잭션 커밋
    // 요구조건: simpleDb.commit()
    public void commit() {
        try {
            Connection conn = threadConnection.get();
            if (conn != null && isOpenTransaction()) {
                conn.commit();
                conn.setAutoCommit(true);
                openTransaction.set(false);

                if (devMode) {
                    System.out.println("Transaction committed");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("커밋 중 오류 발생", e);
        }
    }

    // 트랜잭션 롤백
    // 요구조건: simpleDb.rollback()
    public void rollback() {
        try {
            Connection conn = threadConnection.get();
            if (conn != null && isOpenTransaction()) {
                conn.rollback();
                conn.setAutoCommit(true);
                openTransaction.set(false);

                if (devMode) {
                    System.out.println("Transaction rolled back");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("롤백 중 오류 발생", e);
        }
    }

    // 현재 스레드의 Connection 닫기
    // 요구조건: simpleDb.close()
    public void close() {
        try {
            Connection conn = threadConnection.get();
            if (conn != null && !conn.isClosed()) {
                conn.close();
                threadConnection.remove();
                openTransaction.remove();

                if (devMode) {
                    System.out.println("Connection closed");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Connection 닫기 중 오류 발생", e);
        }
    }

    // SQL conneciton 간접 접근용
    public Connection getSqlConnection() throws SQLException {
        return getConnection();
    }
    
    // devMode 상태값 반환
    boolean isDevMode() {
        return devMode;
    }
}
