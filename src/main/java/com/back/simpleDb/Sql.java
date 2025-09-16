package com.back.simpleDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public Sql append(String sql, Object parameter) {
        if (!sqlBuilder.isEmpty()) {
            sqlBuilder.append(" ");
        }
        sqlBuilder.append(sql);
        parameters.add(parameter);
        return this;
    }

    public long insert() {
        try (Connection conn = simpleDb.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString(), Statement.RETURN_GENERATED_KEYS)) {

            // 파라미터 설정
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }

            // 개발 모드 로그
            if (simpleDb.isDevMode()) {
                System.out.println("== rawSql ==\n" + buildRawSql());
            }

            pstmt.executeUpdate();

            // 생성된 키 반환
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
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