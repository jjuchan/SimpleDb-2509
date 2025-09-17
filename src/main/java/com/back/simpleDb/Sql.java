package com.back.simpleDb;

import java.sql.*;
import java.util.*;

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
        // addAll: 다른 컬렉션의 모든 요소를 현재 리스트에 추가
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

    /*
    여러 행을 Map 리스트로 조회

    구현 로직:
    1. executeQuery()로 SELECT 실행
    2. ResultSet의 각 행을 Map으로 변환
    3. 모든 행을 List에 담아 반환

    executeQuery(): SELECT 전용, ResultSet 반환
    ResultSet.next(): 다음 행으로 이동, 없으면 false
    LinkedHashMap: 컬럼 순서 유지
    */
    public List<Map<String, Object>> selectRows() {
        List<Map<String, Object>> rows = new ArrayList<>();

        try {
            Connection conn = simpleDb.getSqlConnection();
            String sql = sqlBuilder.toString();

            if (simpleDb.isDevMode()) {
                System.out.println("SQL: " + sql);
                System.out.println("Parameters: " + parameters);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                setParameters(pstmt);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        rows.add(resultSetToMap(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("SELECT 오류 발생");
        }

        return rows;
    }

    /*
    구현 로직:
    1. selectRows() 호출
    2. 첫 번째 행 반환 (없으면 null)
    */
    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.get(0);
    }

    /*
    ResultSet을 Map으로 변환

    구현 로직:
    1. ResultSetMetaData로 컬럼 정보 가져오기
    2. 각 컬럼 이름과 값을 Map에 저장

    - ResultSetMetaData: 컬럼 이름, 타입, 개수 등 메타정보
    - getColumnCount(): 컬럼 개수
    - getColumnName(i): i번째 컬럼명 (1부터 시작)
    - getObject(i): i번째 컬럼 값 (타입 매핑 자동)
    */
    private Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            Object value = rs.getObject(i);
            map.put(columnName, value);
        }

        return map;
    }
}
