import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LibraryGUI extends JFrame {
    private JTextField titleField, authorField, bookIdField, availabilityField;
    private JTextArea outputArea;

    public LibraryGUI(String adminName) {
        setTitle("Admin Dashboard - Welcome " + adminName);
        setSize(600, 500);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel header = new JLabel("Library Book Management", JLabel.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 18));
        header.setBounds(100, 20, 400, 30);
        add(header);

        JLabel titleLbl = new JLabel("Title:");
        titleLbl.setBounds(50, 80, 100, 25);
        add(titleLbl);
        titleField = new JTextField();
        titleField.setBounds(150, 80, 180, 25);
        add(titleField);

        JLabel authorLbl = new JLabel("Author:");
        authorLbl.setBounds(50, 120, 100, 25);
        add(authorLbl);
        authorField = new JTextField();
        authorField.setBounds(150, 120, 180, 25);
        add(authorField);

        JLabel idLbl = new JLabel("Book ID:");
        idLbl.setBounds(50, 160, 100, 25);
        add(idLbl);
        bookIdField = new JTextField();
        bookIdField.setBounds(150, 160, 180, 25);
        add(bookIdField);

        JLabel availLbl = new JLabel("Availability (true/false):");
        availLbl.setBounds(50, 200, 180, 25);
        add(availLbl);
        availabilityField = new JTextField();
        availabilityField.setBounds(230, 200, 100, 25);
        add(availabilityField);

        JButton addBtn = new JButton("Add Book");
        addBtn.setBounds(50, 250, 120, 30);
        add(addBtn);

        JButton deleteBtn = new JButton("Delete Book");
        deleteBtn.setBounds(190, 250, 120, 30);
        add(deleteBtn);

        JButton updateBtn = new JButton("Update Availability");
        updateBtn.setBounds(330, 250, 180, 30);
        add(updateBtn);

        JButton refreshBtn = new JButton("View All Books");
        refreshBtn.setBounds(50, 300, 200, 30);
        add(refreshBtn);

        outputArea = new JTextArea();
        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBounds(50, 340, 480, 100);
        add(scroll);

        addBtn.addActionListener(e -> addBook());
        deleteBtn.addActionListener(e -> deleteBook());
        updateBtn.addActionListener(e -> updateAvailability());
        refreshBtn.addActionListener(e -> showAllBooks());

        setVisible(true);
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/librarydb", "root", "Bittu$123"
        );
    }

    private void addBook() {
        String title = titleField.getText();
        String author = authorField.getText();

        if (title.isEmpty() || author.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title and author are required.");
            return;
        }

        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO books (title, author, available) VALUES (?, ?, true)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, title);
            stmt.setString(2, author);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Book added successfully!");
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void deleteBook() {
        String idText = bookIdField.getText();

        try (Connection conn = getConnection()) {
            String sql = "DELETE FROM books WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, Integer.parseInt(idText));
            int rows = stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, rows > 0 ? "Book deleted." : "Book ID not found.");
        } catch (Exception e) {
            showError(e);
        }
    }

    private void updateAvailability() {
        try (Connection conn = getConnection()) {
            int bookId = Integer.parseInt(bookIdField.getText());
            boolean available = Boolean.parseBoolean(availabilityField.getText());

            String sql = "UPDATE books SET available=? WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setBoolean(1, available);
            stmt.setInt(2, bookId);
            int rows = stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, rows > 0 ? "Availability updated." : "Book ID not found.");
        } catch (Exception e) {
            showError(e);
        }
    }

    private void showAllBooks() {
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM books";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            StringBuilder sb = new StringBuilder("📚 All Books:\n");
            while (rs.next()) {
                sb.append("ID: ").append(rs.getInt("id"))
                  .append(", Title: ").append(rs.getString("title"))
                  .append(", Author: ").append(rs.getString("author"))
                  .append(", Available: ").append(rs.getBoolean("available"))
                  .append("\n");
            }
            outputArea.setText(sb.toString());
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        e.printStackTrace();
    }

    public static void main(String[] args) {
        new LibraryGUI("admin");
    }
}
