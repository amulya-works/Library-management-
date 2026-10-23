package com.library;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Neo4jDatabaseConnection {

    private static final String URL = "jdbc:neo4j://127.0.0.1:7687";
    private static final String USERNAME = "neo4j";
    private static final String PASSWORD = System.getenv("NEO4J_PASSWORD");

    public static Connection getConnection() throws SQLException {
        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new SQLException("Set the NEO4J_PASSWORD environment variable first.");
        }

        try {
            Class.forName("org.neo4j.jdbc.Neo4jDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Neo4j JDBC driver class could not be loaded.", e);
        }

        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}