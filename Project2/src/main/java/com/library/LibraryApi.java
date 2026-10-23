package com.library;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class LibraryApi {

    private static final Path STATIC_DIR =
        Path.of("src", "main", "resources", "static");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 8080), 0
        );

        server.createContext("/api/stats", LibraryApi::getStats);
        server.createContext("/api/borrowings", LibraryApi::getBorrowings);
        server.createContext("/api/ratings", LibraryApi::getRatings);
        server.createContext("/api/books/add", LibraryApi::addBook);
        server.createContext("/api/books/update", LibraryApi::updateBook);
        server.createContext("/api/books/delete", LibraryApi::deleteBook);
        server.createContext("/api/books", LibraryApi::getBooks);
        server.createContext("/", LibraryApi::serveStaticFiles);
        

        server.start();

        System.out.println("Library website running at http://127.0.0.1:8080");
    }

    private static void getStats(HttpExchange exchange) throws IOException {
        String response;
        int status = 200;

        try (
            Connection connection = Neo4jDatabaseConnection.getConnection();
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(
                "MATCH (b:Book) " +
                "WITH count(b) AS books " +
                "MATCH (m:Member) " +
                "WITH books, count(m) AS members " +
                "MATCH (r:Rating) " +
                "RETURN books, members, count(r) AS ratings"
            )
        ) {
            if (result.next()) {
                response = "{\"books\":" + result.getLong("books")
                    + ",\"members\":" + result.getLong("members")
                    + ",\"ratings\":" + result.getLong("ratings") + "}";
            } else {
                status = 500;
                response = "{\"error\":\"No statistics available\"}";
            }
        } catch (Exception e) {
            status = 500;
            response = "{\"error\":\"Database query failed\"}";
            e.printStackTrace();
        }

        sendResponse(exchange, status, "application/json; charset=UTF-8", response);
    }

    private static void serveStaticFiles(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();

        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        Path file = STATIC_DIR.resolve(requestPath.substring(1)).normalize();

        if (!file.startsWith(STATIC_DIR.normalize()) || !Files.isRegularFile(file)) {
            sendResponse(exchange, 404, "text/plain; charset=UTF-8", "404 Not Found");
            return;
        }

        String contentType = "text/plain; charset=UTF-8";

        if (requestPath.endsWith(".html")) {
            contentType = "text/html; charset=UTF-8";
        } else if (requestPath.endsWith(".css")) {
            contentType = "text/css; charset=UTF-8";
        } else if (requestPath.endsWith(".js")) {
            contentType = "application/javascript; charset=UTF-8";
        }

        byte[] bytes = Files.readAllBytes(file);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void sendResponse(
        HttpExchange exchange,
        int status,
        String contentType,
        String response
    ) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
    private static void updateBook(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        exchange.getResponseHeaders().set("Allow", "POST");
        sendResponse(exchange, 405, "application/json; charset=UTF-8",
            "{\"error\":\"Use POST to update a book.\"}");
        return;
    }

    String body = new String(
        exchange.getRequestBody().readAllBytes(),
        StandardCharsets.UTF_8
    );

    String bookId = null;
    String newTitle = null;

    try {
        for (String item : body.split("&")) {
            String[] pair = item.split("=", 2);

            if (pair.length != 2) {
                continue;
            }

            String key = java.net.URLDecoder.decode(
                pair[0], StandardCharsets.UTF_8
            );

            String value = java.net.URLDecoder.decode(
                pair[1], StandardCharsets.UTF_8
            );

            if (key.equals("bookId")) {
                bookId = value.trim();
            } else if (key.equals("newTitle")) {
                newTitle = value.trim();
            }
        }
    } catch (Exception e) {
        sendResponse(exchange, 400, "application/json; charset=UTF-8",
            "{\"error\":\"Invalid request data.\"}");
        return;
    }

    if (bookId == null || bookId.isBlank()
            || newTitle == null || newTitle.isBlank()) {
        sendResponse(exchange, 400, "application/json; charset=UTF-8",
            "{\"error\":\"Book ID and new title are required.\"}");
        return;
    }

    try (
        Connection connection = Neo4jDatabaseConnection.getConnection();
        java.sql.PreparedStatement statement = connection.prepareStatement(
            "MATCH (b:Book) WHERE toString(b.BookID) = ? " +
            "SET b.BookTitle = ? RETURN b.BookID AS id"
        )
    ) {
        statement.setString(1, bookId);
        statement.setString(2, newTitle);

        try (ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                sendResponse(exchange, 200,
                    "application/json; charset=UTF-8",
                    "{\"message\":\"Book title updated successfully.\"}");
            } else {
                sendResponse(exchange, 404,
                    "application/json; charset=UTF-8",
                    "{\"error\":\"Book ID not found.\"}");
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, 500,
            "application/json; charset=UTF-8",
            "{\"error\":\"Could not update book title.\"}");
    }
}
private static void getBooks(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
        sendResponse(exchange, 405, "application/json; charset=UTF-8",
            "{\"error\":\"Method not allowed.\"}");
        return;
    }

    String searchTerm = "";
    String query = exchange.getRequestURI().getRawQuery();

    if (query != null && query.startsWith("search=")) {
        searchTerm = java.net.URLDecoder.decode(
            query.substring(7), StandardCharsets.UTF_8
        ).trim();
    }

    String escapedSearch = searchTerm
        .replace("\\", "\\\\")
        .replace("'", "\\'");

    String cypher =
        "MATCH (b:Book) " +
        "WHERE ('" + escapedSearch + "' = '' OR " +
        "toLower(toString(b.BookTitle)) CONTAINS toLower('" + escapedSearch + "') OR " +
        "toLower(toString(b.ISBN)) CONTAINS toLower('" + escapedSearch + "')) " +
        "RETURN b.BookID AS id, b.BookTitle AS title, " +
        "b.ISBN AS isbn, b.PublisherID AS publisherId " +
        "ORDER BY b.BookID";

    try (
        Connection connection = Neo4jDatabaseConnection.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(cypher)
    ) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        while (result.next()) {
            if (!first) {
                json.append(",");
            }

            json.append("{")
                .append("\"id\":\"").append(escapeJson(result.getString("id"))).append("\",")
                .append("\"title\":\"").append(escapeJson(result.getString("title"))).append("\",")
                .append("\"isbn\":\"").append(escapeJson(result.getString("isbn"))).append("\",")
                .append("\"publisherId\":\"").append(escapeJson(result.getString("publisherId"))).append("\"")
                .append("}");

            first = false;
        }

        json.append("]");

        sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());

    } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, 500, "application/json; charset=UTF-8",
            "{\"error\":\"Could not retrieve books.\"}");
    }
}

private static String escapeJson(String value) {
    if (value == null) {
        return "";
    }

    return value.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
}
private static void addBook(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        exchange.getResponseHeaders().set("Allow", "POST");
        sendResponse(exchange, 405, "application/json; charset=UTF-8",
            "{\"error\":\"Use POST to add a book.\"}");
        return;
    }

    String body = new String(
        exchange.getRequestBody().readAllBytes(),
        StandardCharsets.UTF_8
    );

    String bookId = null;
    String title = null;
    String isbn = null;
    String publisherId = null;

    try {
        for (String item : body.split("&")) {
            String[] pair = item.split("=", 2);

            if (pair.length != 2) {
                continue;
            }

            String key = java.net.URLDecoder.decode(
                pair[0], StandardCharsets.UTF_8
            );

            String value = java.net.URLDecoder.decode(
                pair[1], StandardCharsets.UTF_8
            ).trim();

            switch (key) {
                case "bookId" -> bookId = value;
                case "title" -> title = value;
                case "isbn" -> isbn = value;
                case "publisherId" -> publisherId = value;
            }
        }
    } catch (Exception e) {
        sendResponse(exchange, 400, "application/json; charset=UTF-8",
            "{\"error\":\"Invalid request data.\"}");
        return;
    }

    if (bookId == null || title == null || isbn == null
            || publisherId == null || bookId.isBlank()
            || title.isBlank() || isbn.isBlank()
            || publisherId.isBlank()) {
        sendResponse(exchange, 400, "application/json; charset=UTF-8",
            "{\"error\":\"All fields are required.\"}");
        return;
    }

    int id;
    int pubId;

    try {
        id = Integer.parseInt(bookId);
        pubId = Integer.parseInt(publisherId);
    } catch (NumberFormatException e) {
        sendResponse(exchange, 400, "application/json; charset=UTF-8",
            "{\"error\":\"Book ID and Publisher ID must be integers.\"}");
        return;
    }

    String escapedTitle = title.replace("\\", "\\\\").replace("'", "\\'");
    String escapedIsbn = isbn.replace("\\", "\\\\").replace("'", "\\'");

    try (Connection connection = Neo4jDatabaseConnection.getConnection();
         Statement statement = connection.createStatement()) {

        String duplicateQuery =
            "MATCH (b:Book {BookID: " + id + "}) RETURN count(b) AS total";

        try (ResultSet result = statement.executeQuery(duplicateQuery)) {
            if (result.next() && result.getLong("total") > 0) {
                sendResponse(exchange, 409, "application/json; charset=UTF-8",
                    "{\"error\":\"This Book ID already exists.\"}");
                return;
            }
        }

        String publisherQuery =
            "MATCH (p:Publisher {PublisherID: " + pubId + "}) " +
            "RETURN count(p) AS total";

        try (ResultSet result = statement.executeQuery(publisherQuery)) {
            if (!result.next() || result.getLong("total") == 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8",
                    "{\"error\":\"Publisher ID does not exist.\"}");
                return;
            }
        }

        String createQuery =
            "MATCH (p:Publisher {PublisherID: " + pubId + "}) " +
            "CREATE (b:Book {BookID: " + id +
            ", BookTitle: '" + escapedTitle +
            "', ISBN: '" + escapedIsbn +
            "', PublisherID: " + pubId +
            "})-[:PUBLISHED_BY]->(p) " +
            "RETURN b.BookID AS id";

        try (ResultSet result = statement.executeQuery(createQuery)) {
            if (result.next()) {
                sendResponse(exchange, 201,
                    "application/json; charset=UTF-8",
                    "{\"message\":\"Book added successfully.\"}");
            } else {
                sendResponse(exchange, 500,
                    "application/json; charset=UTF-8",
                    "{\"error\":\"Could not create the book.\"}");
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, 500, "application/json; charset=UTF-8",
            "{\"error\":\"Could not add book to the database.\"}");
    }
}
private static void getBorrowings(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
        exchange.getResponseHeaders().set("Allow", "GET");
        sendResponse(exchange, 405, "application/json; charset=UTF-8",
            "{\"error\":\"Method not allowed.\"}");
        return;
    }

    String cypher =
        "MATCH (m:Member)-[:MADE_BORROWING]->(br:Borrowing) " +
        "OPTIONAL MATCH (br)-[:BORROWED_BOOK]->(b:Book) " +
        "OPTIONAL MATCH (br)-[:PROCESSED_BY]->(l:Librarian) " +
        "RETURN br.BorrowingID AS borrowingId, " +
        "m.MemberID AS memberId, " +
        "trim(coalesce(m.FirstName, '') + ' ' + coalesce(m.LastName, '')) AS memberName, " +
        "br.BookID AS bookId, " +
        "b.BookTitle AS bookTitle, " +
        "br.LibrarianID AS librarianId, " +
        "br.IssueDate AS issueDate, " +
        "br.DueDate AS dueDate, " +
        "br.ReturnDate AS returnDate, " +
        "br.Status AS status, " +
        "br.FineAmount AS fineAmount " +
        "ORDER BY br.BorrowingID";

    try (
        Connection connection = Neo4jDatabaseConnection.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(cypher)
    ) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        while (result.next()) {
            if (!first) {
                json.append(",");
            }

            json.append("{")
                .append("\"borrowingId\":\"").append(escapeJson(result.getString("borrowingId"))).append("\",")
                .append("\"memberId\":\"").append(escapeJson(result.getString("memberId"))).append("\",")
                .append("\"memberName\":\"").append(escapeJson(result.getString("memberName"))).append("\",")
                .append("\"bookId\":\"").append(escapeJson(result.getString("bookId"))).append("\",")
                .append("\"bookTitle\":\"").append(escapeJson(result.getString("bookTitle"))).append("\",")
                .append("\"librarianId\":\"").append(escapeJson(result.getString("librarianId"))).append("\",")
                .append("\"issueDate\":\"").append(escapeJson(result.getString("issueDate"))).append("\",")
                .append("\"dueDate\":\"").append(escapeJson(result.getString("dueDate"))).append("\",")
                .append("\"returnDate\":\"").append(escapeJson(result.getString("returnDate"))).append("\",")
                .append("\"status\":\"").append(escapeJson(result.getString("status"))).append("\",")
                .append("\"fineAmount\":\"").append(escapeJson(result.getString("fineAmount"))).append("\"")
                .append("}");

            first = false;
        }

        json.append("]");

        sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());

    } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, 500, "application/json; charset=UTF-8",
            "{\"error\":\"Could not retrieve borrowing records.\"}");
    }
}
private static void getRatings(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
        exchange.getResponseHeaders().set("Allow", "GET");
        sendResponse(exchange, 405, "application/json; charset=UTF-8",
            "{\"error\":\"Method not allowed.\"}");
        return;
    }

    String cypher =
        "MATCH (m:Member)-[:GAVE_RATING]->(r:Rating) " +
        "RETURN r.RatingID AS ratingId, " +
        "m.MemberID AS memberId, " +
        "trim(coalesce(m.FirstName, '') + ' ' + coalesce(m.LastName, '')) AS memberName, " +
        "r.LibrarianID AS librarianId, " +
        "r.Rating AS rating, " +
        "r.Review AS review, " +
        "r.RatingDate AS ratingDate " +
        "ORDER BY r.RatingID";

    try (
        Connection connection = Neo4jDatabaseConnection.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(cypher)
    ) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        while (result.next()) {
            if (!first) {
                json.append(",");
            }

            json.append("{")
                .append("\"ratingId\":\"").append(escapeJson(result.getString("ratingId"))).append("\",")
                .append("\"memberId\":\"").append(escapeJson(result.getString("memberId"))).append("\",")
                .append("\"memberName\":\"").append(escapeJson(result.getString("memberName"))).append("\",")
                .append("\"librarianId\":\"").append(escapeJson(result.getString("librarianId"))).append("\",")
                .append("\"rating\":\"").append(escapeJson(result.getString("rating"))).append("\",")
                .append("\"review\":\"").append(escapeJson(result.getString("review"))).append("\",")
                .append("\"ratingDate\":\"").append(escapeJson(result.getString("ratingDate"))).append("\"")
                .append("}");

            first = false;
        }

        json.append("]");

        sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());

    } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, 500, "application/json; charset=UTF-8",
            "{\"error\":\"Could not retrieve ratings.\"}");
    }
}
private static void deleteBook(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        exchange.getResponseHeaders().set("Allow", "POST");
        sendResponse(exchange, 405, "application/json; charset=UTF-8",
            "{\"error\":\"Use POST to delete a book.\"}");
        return;
    }

    String body = new String(
        exchange.getRequestBody().readAllBytes(),
        StandardCharsets.UTF_8
    );

    String bookId = null;

    for (String item : body.split("&")) {
        String[] pair = item.split("=", 2);

        if (pair.length == 2) {
            String key = java.net.URLDecoder.decode(
                pair[0], StandardCharsets.UTF_8
            );

            String value = java.net.URLDecoder.decode(
                pair[1], StandardCharsets.UTF_8
            );

            if (key.equals("bookId")) {
                bookId = value.trim();
            }
        }
    }

    if (bookId == null || !bookId.matches("\\d+")) {
        sendResponse(exchange, 400, "application/json; charset=UTF-8",
            "{\"error\":\"A valid Book ID is required.\"}");
        return;
    }

    try (
        Connection connection = Neo4jDatabaseConnection.getConnection();
        Statement statement = connection.createStatement()
    ) {
        String query =
            "MATCH (b:Book) WHERE toString(b.BookID) = '" + bookId + "' " +
            "WITH b, b.BookID AS id " +
            "DETACH DELETE b " +
            "RETURN id";

        try (ResultSet result = statement.executeQuery(query)) {
            if (result.next()) {
                sendResponse(exchange, 200, "application/json; charset=UTF-8",
                    "{\"message\":\"Book deleted successfully.\"}");
            } else {
                sendResponse(exchange, 404, "application/json; charset=UTF-8",
                    "{\"error\":\"Book ID not found.\"}");
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, 500, "application/json; charset=UTF-8",
            "{\"error\":\"Could not delete book.\"}");
    }
}
}