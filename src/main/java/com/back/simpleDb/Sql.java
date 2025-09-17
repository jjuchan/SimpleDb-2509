package com.back.simpleDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> parameters = new ArrayList<>();

    // SimpleDb.java의 객체 생성
    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql) {
        if(sqlBuilder.length() > 0) {
            sqlBuilder.append(" ");
        }
        sqlBuilder.append(sql);
        // this가 의미하는것은 Class Sql
        return this;
    }

    public Sql append(String sql, Object... params) {
        append(sql);
        // addAll 메서드는 어디서 제공되는것이고, 어떤 기능인가?
        // 배열과 리스트 차이점은?
        parameters.addAll(Arrays.asList(params));
        return this;
    }

    // Statement vs PreparedStatement: 파싱으로 인한 속도차이 1000건 insert할 경우 1000ms vs 100ms 
    // setObject(): sql injection 방어
    private void setParameters(PreparedStatement pstmt) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            pstmt.setObject(i + 1, parameters.get(i));
        }
    }

    // INSERT 메서드
    public long insert() {
        try{
            Connection conn = simpleDb.getSqlConnection();
            String sql = sqlBuilder.toString();

            if(simpleDb.isDevMode()) {
                System.out.println("SQL: " + sql);
                System.out.println("Parameters: " + parameters);
            }

            // AUTO_INCREMENT ID 요청 -> Statement.RETURN_GENERATED_KEYS
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setParameters(pstmt);
                pstmt.executeUpdate();

                // AUTO_INCREMENT ID 반환
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("INSERT 오류 발생", e);
        }
    }

    // UPDATE 메서드
    public int update() {
        return executeUpdate();
    }

    // DELETE 메서드
    public int delete() {
        return executeUpdate();
    }

    // INSERT/UPDATE/DELETE 쿼리 실행 공통 메서드
    private int executeUpdate() {
        try {
            Connection conn = simpleDb.getSqlConnection();
            String sql = sqlBuilder.toString();

            if (simpleDb.isDevMode()) {
                System.out.println("SQL: " + sql);
                System.out.println("Parameters: " + parameters);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                setParameters(pstmt);
                return pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("SQL 실행 오류");
        }
    }
}
