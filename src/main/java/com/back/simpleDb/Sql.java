package com.back.simpleDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder;
    private final List<Object> parameters;

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
        this.sqlBuilder = new StringBuilder();
        this.parameters = new ArrayList<>();
    }

    public Sql append(String sql) {
        if (!sqlBuilder.isEmpty()) {
            sqlBuilder.append(" ");
        }
        sqlBuilder.append(sql);
        return this;
    }

    public Sql append(String sql, Object... parameter) {
        if (!sqlBuilder.isEmpty()) {
            sqlBuilder.append(" ");
        }
        sqlBuilder.append(sql);
        for (Object param : parameter) {
            parameters.add(param);
        }
        return this;
    }

    public long insert() {
        try (Connection conn = simpleDb.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString(), Statement.RETURN_GENERATED_KEYS)) {

            bindParametersAndLog(pstmt);

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int update() {
        try (Connection conn = simpleDb.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {

            bindParametersAndLog(pstmt);

            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int delete() {
        try (Connection conn = simpleDb.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {

            bindParametersAndLog(pstmt);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> selectRows() {
        try (Connection conn = simpleDb.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {

            bindParametersAndLog(pstmt);

            try (ResultSet rs = pstmt.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        Object value = rs.getObject(i);
                        row.put(meta.getColumnName(i),
                                value instanceof Timestamp ? ((Timestamp) value).toLocalDateTime() :
                                        value instanceof byte[] && ((byte[]) value).length == 1 ? ((byte[]) value)[0] != 0 : value);
                    }
                    rows.add(row);
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * PreparedStatement에 파라미터를 바인딩하고 개발 모드일 때 로그를 출력하는 공통 메서드
     **/
    private void bindParametersAndLog(PreparedStatement pstmt) throws SQLException {
        // 파라미터 설정
        for (int i = 0; i < parameters.size(); i++) {
            pstmt.setObject(i + 1, parameters.get(i));
        }

        // 개발 모드 로그
        if (simpleDb.isDevMode()) {
            System.out.println("== rawSql ==\n" + buildRawSql());
        }
    }

    private String buildRawSql() {
        String result = sqlBuilder.toString();
        for (Object param : parameters) {
            if (param instanceof String) {
                result = result.replaceFirst("\\?", "'" + param + "'");
            } else {
                result = result.replaceFirst("\\?", String.valueOf(param));
            }
        }
        return result;
    }
}