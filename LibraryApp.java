import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class LibraryApp extends JFrame {
    private JTable bookTable;

    public LibraryApp() {
        setTitle("Library Management System");
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.addColumn("Book ID");
        tableModel.addColumn("Book Title");
        tableModel.addColumn("ISBN");
        tableModel.addColumn("Publisher ID");

        bookTable = new JTable(tableModel);
        add(new JScrollPane(bookTable), BorderLayout.CENTER);

        loadBookData(tableModel);
    }

    private void loadBookData(DefaultTableModel model) {
        String query = "SELECT * FROM book";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int id = rs.getInt("BookID");
                String title = rs.getString("BookTitle");
                String isbn = rs.getString("ISBN");
                int publisherId = rs.getInt("PublisherID");

                model.addRow(new Object[]{id, title, isbn, publisherId});
            }

            System.out.println("Loaded " + model.getRowCount() + " books into the UI.");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error connecting to database: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LibraryApp().setVisible(true);
        });
    }
}