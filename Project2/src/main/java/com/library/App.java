package com.library;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class App {


public static void main(String[] args) {
    try (
        Connection connection = Neo4jDatabaseConnection.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(
            "MATCH (n) RETURN count(n) AS total"
        )
    ) {
        if (result.next()) {
            System.out.println("JDBC connection successful!");
            System.out.println("Total nodes: " + result.getLong("total"));
        }
    } catch (Exception e) {
        System.out.println("Connection failed: " + e.getMessage());
    }
}


}
