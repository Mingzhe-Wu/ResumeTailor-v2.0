package com.mingzhe.resumetailor.rag;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PgVectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(
            PreparedStatement ps,
            int i,
            float[] parameter,
            JdbcType jdbcType
    ) throws SQLException {
        PGobject vectorObject = new PGobject();
        vectorObject.setType("vector");
        vectorObject.setValue(toVectorString(parameter));

        ps.setObject(i, vectorObject);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseVector(rs.getString(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseVector(rs.getString(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseVector(cs.getString(columnIndex));
    }

    private String toVectorString(float[] vector) {
        if (vector == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }

        sb.append("]");
        return sb.toString();
    }

    private float[] parseVector(String value) throws SQLException {
        if (value == null || value.isBlank()) {
            return null;
        }

        String cleaned = value.trim();

        if (!cleaned.startsWith("[") || !cleaned.endsWith("]")) {
            throw new SQLException("Invalid pgvector format: " + value);
        }

        cleaned = cleaned.substring(1, cleaned.length() - 1).trim();

        if (cleaned.isEmpty()) {
            return new float[0];
        }

        String[] parts = cleaned.split(",");
        float[] vector = new float[parts.length];

        try {
            for (int i = 0; i < parts.length; i++) {
                vector[i] = Float.parseFloat(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            throw new SQLException("Failed to parse pgvector value: " + value, e);
        }

        return vector;
    }
}