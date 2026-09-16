import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class LibrarianWebServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/", new DashboardHandler());
        server.createContext("/add-book", new AddBookHandler());
        server.createContext("/update-book", new UpdateBookHandler());
        server.createContext("/delete-book", new DeleteBookHandler());
        server.createContext("/rate", new AddRatingHandler());
        server.createContext("/update-borrowing", new UpdateBorrowingHandler());

        server.setExecutor(null);
        System.out.println("Librarian Web App running at http://localhost:8080/");
        server.start();
    }

    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder bookRows = new StringBuilder();
            StringBuilder borrowingRows = new StringBuilder();
            StringBuilder ratingRows = new StringBuilder();
            int totalBooks = 0;
            int totalBorrowings = 0;
            int totalRatings = 0;

            try (Connection conn = DatabaseConnection.getConnection()) {
                Statement stmt = conn.createStatement();
                
                // Fetch Books
                ResultSet rs = stmt.executeQuery("SELECT * FROM BOOK");
                while (rs.next()) {
                    totalBooks++;
                    int id = rs.getInt("BookID");
                    String title = rs.getString("BookTitle");
                    String isbn = rs.getString("ISBN");
                    int pubId = rs.getInt("PublisherID");

                    bookRows.append("<tr>")
                            .append("<td><strong>#").append(id).append("</strong></td>")
                            .append("<td>").append(title).append("</td>")
                            .append("<td><code>").append(isbn).append("</code></td>")
                            .append("<td><span class='badge'>Pub #").append(pubId).append("</span></td>")
                            .append("<td style='text-align:right;'>")
                            .append("<button onclick='openEditBook(").append(id).append(", \"").append(title.replace("\"", "&quot;")).append("\", \"").append(isbn).append("\")' class='btn-secondary'>Edit</button> ")
                            .append("<form method='POST' action='/delete-book' style='display:inline;'><input type='hidden' name='bookId' value='").append(id).append("'/><button type='submit' class='btn-danger'>Delete</button></form>")
                            .append("</td></tr>");
                }

                // Fetch Borrowings
                ResultSet rsBorrow = stmt.executeQuery("SELECT * FROM BORROWING");
                while (rsBorrow.next()) {
                    totalBorrowings++;
                    int borrowId = rsBorrow.getInt("BorrowingID");
                    String status = rsBorrow.getString("Status");
                    String statusClass = "Issued".equalsIgnoreCase(status) ? "badge-warning" : ("Overdue".equalsIgnoreCase(status) ? "badge-danger" : "badge-success");
                    
                    borrowingRows.append("<tr>")
                            .append("<td><strong>#").append(borrowId).append("</strong></td>")
                            .append("<td>Member #").append(rsBorrow.getInt("MemberID")).append("</td>")
                            .append("<td>Book #").append(rsBorrow.getInt("BookID")).append("</td>")
                            .append("<td>").append(rsBorrow.getDate("IssueDate")).append("</td>")
                            .append("<td>").append(rsBorrow.getDate("DueDate")).append("</td>")
                            .append("<td>").append(rsBorrow.getDate("ReturnDate") == null ? "-" : rsBorrow.getDate("ReturnDate")).append("</td>")
                            .append("<td><span class='badge ").append(statusClass).append("'>").append(status).append("</span></td>")
                            .append("<td>$").append(rsBorrow.getBigDecimal("FineAmount")).append("</td>")
                            .append("<td style='text-align:right;'>")
                            .append("<button onclick='editBorrowing(").append(borrowId).append(", \"").append(status).append("\")' class='btn-secondary'>Update Status</button>")
                            .append("</td></tr>");
                }

                // Fetch Ratings
                ResultSet rsRatings = stmt.executeQuery("SELECT * FROM RATING");
                while (rsRatings.next()) {
                    totalRatings++;
                    int stars = rsRatings.getInt("Rating");
                    String starStr = "★".repeat(stars) + "☆".repeat(5 - stars);
                    ratingRows.append("<tr>")
                            .append("<td><strong>#").append(rsRatings.getInt("RatingID")).append("</strong></td>")
                            .append("<td>Member #").append(rsRatings.getInt("MemberID")).append("</td>")
                            .append("<td>Librarian #").append(rsRatings.getInt("LibrarianID")).append("</td>")
                            .append("<td class='stars'>").append(starStr).append("</td>")
                            .append("<td>").append(rsRatings.getString("Review") == null ? "-" : rsRatings.getString("Review")).append("</td>")
                            .append("</tr>");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            String html = "<!DOCTYPE html><html><head><title>Librarian Admin Portal</title>" +
                    "<meta charset='UTF-8'>" +
                    "<link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap' rel='stylesheet'>" +
                    "<style>" +
                    "* { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }" +
                    "body { background: #f8fafc; color: #1e293b; display: flex; min-height: 100vh; }" +
                    ".sidebar { width: 260px; background: #0f172a; color: #fff; padding: 24px; flex-shrink: 0; }" +
                    ".sidebar h2 { font-size: 1.25rem; font-weight: 700; color: #38bdf8; margin-bottom: 32px; }" +
                    ".nav-item { display: block; padding: 12px 16px; color: #94a3b8; text-decoration: none; border-radius: 8px; font-weight: 500; margin-bottom: 8px; cursor: pointer; transition: all 0.2s; }" +
                    ".nav-item:hover, .nav-item.active { background: #1e293b; color: #38bdf8; }" +
                    ".main-content { flex: 1; padding: 40px; overflow-y: auto; }" +
                    ".header { margin-bottom: 32px; }" +
                    ".header h1 { font-size: 1.75rem; font-weight: 700; }" +
                    ".stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 32px; }" +
                    ".stat-card { background: #fff; border: 1px solid #e2e8f0; padding: 20px; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }" +
                    ".stat-card span { font-size: 0.875rem; color: #64748b; font-weight: 500; }" +
                    ".stat-card h3 { font-size: 1.75rem; margin-top: 8px; font-weight: 700; color: #0f172a; }" +
                    ".card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 24px; margin-bottom: 32px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }" +
                    ".card h2 { font-size: 1.125rem; font-weight: 600; margin-bottom: 20px; color: #0f172a; }" +
                    ".form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; align-items: end; }" +
                    ".form-group { display: flex; flex-direction: column; gap: 6px; }" +
                    ".form-group label { font-size: 0.85rem; font-weight: 600; color: #475569; }" +
                    "input, select { padding: 10px 14px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 0.9rem; outline: none; }" +
                    "input:focus, select:focus { border-color: #0284c7; box-shadow: 0 0 0 3px rgba(2,132,199,0.1); }" +
                    "button { padding: 10px 18px; border: none; border-radius: 6px; font-weight: 600; cursor: pointer; background: #0284c7; color: #fff; }" +
                    "button:hover { background: #0369a1; }" +
                    ".btn-secondary { background: #64748b; padding: 6px 12px; font-size: 0.8rem; }" +
                    ".btn-secondary:hover { background: #475569; }" +
                    ".btn-danger { background: #ef4444; padding: 6px 12px; font-size: 0.8rem; }" +
                    ".btn-danger:hover { background: #dc2626; }" +
                    "table { width: 100%; border-collapse: collapse; text-align: left; font-size: 0.9rem; }" +
                    "th { background: #f8fafc; padding: 12px 16px; font-weight: 600; color: #475569; border-bottom: 1px solid #e2e8f0; }" +
                    "td { padding: 14px 16px; border-bottom: 1px solid #f1f5f9; color: #334155; }" +
                    "tr:hover td { background: #f8fafc; }" +
                    ".badge { background: #e0f2fe; color: #0369a1; padding: 4px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: 600; }" +
                    ".badge-warning { background: #fef3c7; color: #b45309; }" +
                    ".badge-success { background: #dcfce7; color: #15803d; }" +
                    ".badge-danger { background: #fee2e2; color: #b91c1c; }" +
                    ".stars { color: #f59e0b; font-size: 1.1rem; letter-spacing: 2px; }" +
                    "code { font-family: monospace; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; font-size: 0.85rem; }" +
                    ".section { display: none; }" +
                    ".section.active { display: block; }" +
                    ".modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); justify-content: center; align-items: center; z-index: 100; }" +
                    ".modal-content { background: #fff; padding: 24px; border-radius: 12px; width: 420px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); }" +
                    "</style></head><body>" +

                    "<!-- Side Navigation Menu -->" +
                    "<div class='sidebar'>" +
                    "<h2>📚 Library Portal</h2>" +
                    "<a class='nav-item active' onclick='showSection(\"dashboard-sec\", this)'>📊 Dashboard</a>" +
                    "<a class='nav-item' onclick='showSection(\"catalog-sec\", this)'>📖 Book Catalog</a>" +
                    "<a class='nav-item' onclick='showSection(\"addbook-sec\", this)'>➕ Add New Book</a>" +
                    "<a class='nav-item' onclick='showSection(\"borrowings-sec\", this)'>📋 Borrowing Tracker</a>" +
                    "<a class='nav-item' onclick='showSection(\"ratings-sec\", this)'>⭐ Ratings & Reviews</a>" +
                    "</div>" +

                    "<div class='main-content'>" +

                    "<!-- Section 1: Dashboard -->" +
                    "<div id='dashboard-sec' class='section active'>" +
                    "<div class='header'><h1>Dashboard Overview</h1></div>" +
                    "<div class='stats-grid'>" +
                    "<div class='stat-card'><span>Total Catalog Books</span><h3>" + totalBooks + "</h3></div>" +
                    "<div class='stat-card'><span>Active Borrowings</span><h3>" + totalBorrowings + "</h3></div>" +
                    "<div class='stat-card'><span>Total Ratings Submitted</span><h3>" + totalRatings + "</h3></div>" +
                    "</div>" +
                    "</div>" +

                    "<!-- Section 2: Book Catalog -->" +
                    "<div id='catalog-sec' class='section'>" +
                    "<div class='card'>" +
                    "<h2>📖 Book Catalog</h2>" +
                    "<table><thead><tr><th>ID</th><th>Book Title</th><th>ISBN</th><th>Publisher</th><th style='text-align:right;'>Action</th></tr></thead>" +
                    "<tbody>" + bookRows.toString() + "</tbody></table>" +
                    "</div></div>" +

                    "<!-- Section 3: Add New Book -->" +
                    "<div id='addbook-sec' class='section'>" +
                    "<div class='card'>" +
                    "<h2>➕ Add New Book</h2>" +
                    "<form method='POST' action='/add-book' class='form-grid'>" +
                    "<div class='form-group'><label>Book ID</label><input type='number' name='bookId' placeholder='e.g. 9001' required/></div>" +
                    "<div class='form-group'><label>Title</label><input type='text' name='title' placeholder='Book Title' required/></div>" +
                    "<div class='form-group'><label>ISBN</label><input type='text' name='isbn' placeholder='978...' required/></div>" +
                    "<div class='form-group'><label>Publisher ID</label><input type='number' name='publisherId' placeholder='e.g. 1' required/></div>" +
                    "<button type='submit'>Save Book</button>" +
                    "</form>" +
                    "</div></div>" +

                    "<!-- Section 4: Borrowing Tracker -->" +
                    "<div id='borrowings-sec' class='section'>" +
                    "<div class='card'>" +
                    "<h2>📋 Borrowing Tracker & Fines</h2>" +
                    "<table><thead><tr><th>ID</th><th>Member</th><th>Book</th><th>Issue Date</th><th>Due Date</th><th>Return Date</th><th>Status</th><th>Fine</th><th style='text-align:right;'>Action</th></tr></thead>" +
                    "<tbody>" + borrowingRows.toString() + "</tbody></table>" +
                    "</div></div>" +

                    "<!-- Section 5: Ratings & Reviews -->" +
                    "<div id='ratings-sec' class='section'>" +
                    "<div class='card'>" +
                    "<h2>⭐ Submit Member Rating</h2>" +
                    "<form method='POST' action='/rate' class='form-grid'>" +
                    "<div class='form-group'><label>Member ID</label><input type='number' name='memberId' placeholder='1' required/></div>" +
                    "<div class='form-group'><label>Librarian ID</label><input type='number' name='librarianId' placeholder='1' required/></div>" +
                    "<div class='form-group'><label>Rating (1-5)</label><input type='number' name='rating' min='1' max='5' placeholder='5' required/></div>" +
                    "<div class='form-group'><label>Review</label><input type='text' name='review' placeholder='Feedback...'/></div>" +
                    "<button type='submit'>Submit Rating</button>" +
                    "</form>" +
                    "</div>" +

                    "<div class='card'>" +
                    "<h2>💬 Recent Member Reviews</h2>" +
                    "<table><thead><tr><th>ID</th><th>Member</th><th>Librarian</th><th>Rating</th><th>Review</th></tr></thead>" +
                    "<tbody>" + ratingRows.toString() + "</tbody></table>" +
                    "</div></div>" +

                    "</div>" +

                    "<!-- Edit Book Modal -->" +
                    "<div id='editBookModal' class='modal'><div class='modal-content'>" +
                    "<h2>✏️ Edit Book Details</h2><br/>" +
                    "<form method='POST' action='/update-book'>" +
                    "<input type='hidden' id='modalBookId' name='bookId'/>" +
                    "<div class='form-group'><label>Title</label><input type='text' id='modalBookTitle' name='title' required/></div><br/>" +
                    "<div class='form-group'><label>ISBN</label><input type='text' id='modalBookIsbn' name='isbn' required/></div><br/>" +
                    "<button type='submit'>Save Changes</button> " +
                    "<button type='button' onclick='closeModals()' class='btn-secondary'>Cancel</button>" +
                    "</form>" +
                    "</div></div>" +

                    "<!-- Update Borrowing Modal -->" +
                    "<div id='borrowModal' class='modal'><div class='modal-content'>" +
                    "<h2>📋 Update Borrowing Status</h2><br/>" +
                    "<form method='POST' action='/update-borrowing'>" +
                    "<input type='hidden' id='modalBorrowId' name='borrowingId'/>" +
                    "<div class='form-group'><label>Status</label><select name='status'>" +
                    "<option value='Issued'>Issued</option><option value='Returned'>Returned</option><option value='Overdue'>Overdue</option>" +
                    "</select></div><br/>" +
                    "<div class='form-group'><label>Fine Amount ($)</label><input type='number' step='0.01' name='fine' value='0.00'/></div><br/>" +
                    "<button type='submit'>Save Changes</button> " +
                    "<button type='button' onclick='closeModals()' class='btn-secondary'>Cancel</button>" +
                    "</form>" +
                    "</div></div>" +

                    "<script>" +
                    "function showSection(secId, navElement) {" +
                    "   document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));" +
                    "   document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));" +
                    "   document.getElementById(secId).classList.add('active');" +
                    "   navElement.classList.add('active');" +
                    "}" +
                    "function openEditBook(id, title, isbn) {" +
                    "   document.getElementById('modalBookId').value = id;" +
                    "   document.getElementById('modalBookTitle').value = title;" +
                    "   document.getElementById('modalBookIsbn').value = isbn;" +
                    "   document.getElementById('editBookModal').style.display = 'flex';" +
                    "}" +
                    "function editBorrowing(id, status) {" +
                    "   document.getElementById('modalBorrowId').value = id;" +
                    "   document.getElementById('borrowModal').style.display = 'flex';" +
                    "}" +
                    "function closeModals() {" +
                    "   document.getElementById('editBookModal').style.display = 'none';" +
                    "   document.getElementById('borrowModal').style.display = 'none';" +
                    "}" +
                    "</script>" +
                    "</body></html>";

            sendResponse(exchange, html);
        }
    }

    static class AddBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("INSERT INTO BOOK (BookID, BookTitle, ISBN, PublisherID) VALUES (?, ?, ?, ?)")) {
                    stmt.setInt(1, Integer.parseInt(params.get("bookId")));
                    stmt.setString(2, params.get("title"));
                    stmt.setString(3, params.get("isbn"));
                    stmt.setInt(4, Integer.parseInt(params.get("publisherId")));
                    stmt.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            }
            redirectHome(exchange);
        }
    }

    static class UpdateBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("UPDATE BOOK SET BookTitle = ?, ISBN = ? WHERE BookID = ?")) {
                    stmt.setString(1, params.get("title"));
                    stmt.setString(2, params.get("isbn"));
                    stmt.setInt(3, Integer.parseInt(params.get("bookId")));
                    stmt.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            }
            redirectHome(exchange);
        }
    }

    static class UpdateBorrowingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String status = params.get("status");
                    double fine = Double.parseDouble(params.get("fine"));
                    int id = Integer.parseInt(params.get("borrowingId"));

                    String sql = "Returned".equalsIgnoreCase(status) 
                        ? "UPDATE BORROWING SET Status = ?, FineAmount = ?, ReturnDate = CURRENT_DATE WHERE BorrowingID = ?"
                        : "UPDATE BORROWING SET Status = ?, FineAmount = ? WHERE BorrowingID = ?";

                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, status);
                    stmt.setDouble(2, fine);
                    stmt.setInt(3, id);
                    stmt.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            }
            redirectHome(exchange);
        }
    }

    static class DeleteBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("DELETE FROM BOOK WHERE BookID = ?")) {
                    stmt.setInt(1, Integer.parseInt(params.get("bookId")));
                    stmt.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            }
            redirectHome(exchange);
        }
    }

    static class AddRatingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("INSERT INTO RATING (MemberID, LibrarianID, Rating, Review, RatingDate) VALUES (?, ?, ?, ?, CURRENT_DATE)")) {
                    stmt.setInt(1, Integer.parseInt(params.get("memberId")));
                    stmt.setInt(2, Integer.parseInt(params.get("librarianId")));
                    stmt.setInt(3, Integer.parseInt(params.get("rating")));
                    stmt.setString(4, params.get("review"));
                    stmt.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            }
            redirectHome(exchange);
        }
    }

    private static Map<String, String> parseFormData(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String query = br.readLine();
        Map<String, String> result = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(URLDecoder.decode(entry[0], StandardCharsets.UTF_8), URLDecoder.decode(entry[1], StandardCharsets.UTF_8));
                }
            }
        }
        return result;
    }

    private static void sendResponse(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void redirectHome(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(302, -1);
    }
}