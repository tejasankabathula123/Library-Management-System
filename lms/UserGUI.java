import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;

public class UserGUI extends JFrame {
    private JTextField bookTitleField;
    private JTextArea outputArea;
    private final String username;

    public UserGUI(String username) {
        this.username = username;

        setTitle("User Dashboard - Welcome " + username);
        setSize(600, 550);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel header = new JLabel("Library User Panel", JLabel.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 18));
        header.setBounds(100, 20, 400, 30);
        add(header);

        JLabel bookLbl = new JLabel("Enter Book Title:");
        bookLbl.setBounds(50, 80, 150, 25);
        add(bookLbl);

        bookTitleField = new JTextField();
        bookTitleField.setBounds(180, 80, 250, 25);
        add(bookTitleField);

        JButton issueBtn = new JButton("Issue Book");
        JButton returnBtn = new JButton("Return Book");
        JButton fineBtn = new JButton("Pay Fine");
        JButton showBtn = new JButton("Show Available Books");

        issueBtn.setBounds(50, 130, 120, 30);
        returnBtn.setBounds(180, 130, 120, 30);
        fineBtn.setBounds(310, 130, 120, 30);
        showBtn.setBounds(50, 180, 200, 30);

        add(issueBtn);
        add(returnBtn);
        add(fineBtn);
        add(showBtn);

        outputArea = new JTextArea();
        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBounds(50, 230, 480, 250);
        add(scroll);

        issueBtn.addActionListener(e -> issueBook());
        returnBtn.addActionListener(e -> returnBook());
        fineBtn.addActionListener(e -> payFine());
        showBtn.addActionListener(e -> showAvailableBooks());

        setVisible(true);
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/librarydb", "root", "Bittu$123"
        );
    }

    private void showAvailableBooks() {
        try (Connection conn = getConnection()) {
            String sql = "SELECT title, author FROM books WHERE available = true";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            StringBuilder sb = new StringBuilder(" Available Books:\n");
            while (rs.next()) {
                sb.append("- ").append(rs.getString("title"))
                  .append(" by ").append(rs.getString("author")).append("\n");
            }
            outputArea.setText(sb.toString());
        } catch (Exception e) {
            showError(e);
        }
    }

    private void issueBook() {
    String title = bookTitleField.getText().trim();

    if (title.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Please enter a book title.");
        return;
    }

    try (Connection conn = getConnection()) {
        String checkSql = "SELECT id, available FROM books WHERE title = ?";
        PreparedStatement checkStmt = conn.prepareStatement(checkSql);
        checkStmt.setString(1, title);
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            int bookId = rs.getInt("id");
            boolean available = rs.getBoolean("available");

            if (!available) {
                JOptionPane.showMessageDialog(this, " Book is currently not available.");
                return;
            }

            // ✅ Issue book
            String issueSql = "INSERT INTO issued_books (book_id, user_id, issue_date, fine) VALUES (?, ?, ?, 0)";
            PreparedStatement issueStmt = conn.prepareStatement(issueSql);
            issueStmt.setInt(1, bookId);
            issueStmt.setString(2, username);
            issueStmt.setDate(3, Date.valueOf(LocalDate.now()));
            issueStmt.executeUpdate();

            // 🔄 Update availability to false
            String updateSql = "UPDATE books SET available = false WHERE id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setInt(1, bookId);
            updateStmt.executeUpdate();

            // ✅ Show success message
            JOptionPane.showMessageDialog(this, "Book issued successfully!");
            bookTitleField.setText(""); // clear the field

        } else {
            JOptionPane.showMessageDialog(this, " Book not found.");
        }
    } catch (Exception e) {
        showError(e);
    }
}


    private void returnBook() {
        String title = bookTitleField.getText().trim();

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a book title.");
            return;
        }

        try (Connection conn = getConnection()) {
            String findSql = "SELECT b.id FROM books b JOIN issued_books i ON b.id = i.book_id " +
                             "WHERE b.title = ? AND i.user_id = ? AND i.return_date IS NULL";
            PreparedStatement findStmt = conn.prepareStatement(findSql);
            findStmt.setString(1, title);
            findStmt.setString(2, username);
            ResultSet rs = findStmt.executeQuery();

            if (rs.next()) {
                int bookId = rs.getInt("id");

                // calculate fine
                Date today = Date.valueOf(LocalDate.now());
                long fine = 0;

                String issueDateSql = "SELECT issue_date FROM issued_books WHERE book_id = ? AND user_id = ? AND return_date IS NULL";
                PreparedStatement issueStmt = conn.prepareStatement(issueDateSql);
                issueStmt.setInt(1, bookId);
                issueStmt.setString(2, username);
                ResultSet issueRs = issueStmt.executeQuery();
                if (issueRs.next()) {
                    Date issueDate = issueRs.getDate("issue_date");
                    long days = (today.getTime() - issueDate.getTime()) / (1000 * 60 * 60 * 24);
                    if (days > 7) fine = (days - 7) * 5; // $5 per day after 7 days
                }

                String updateReturn = "UPDATE issued_books SET return_date = ?, fine = ? WHERE book_id = ? AND user_id = ? AND return_date IS NULL";
                PreparedStatement updateStmt = conn.prepareStatement(updateReturn);
                updateStmt.setDate(1, today);
                updateStmt.setLong(2, fine);
                updateStmt.setInt(3, bookId);
                updateStmt.setString(4, username);
                updateStmt.executeUpdate();

                PreparedStatement resetBook = conn.prepareStatement("UPDATE books SET available = true WHERE id = ?");
                resetBook.setInt(1, bookId);
                resetBook.executeUpdate();

                JOptionPane.showMessageDialog(this, " Book returned. Fine due: $" + fine);
            } else {
                JOptionPane.showMessageDialog(this, " No active issue found for this book.");
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void payFine() {
        String title = bookTitleField.getText().trim();

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a book title.");
            return;
        }

        try (Connection conn = getConnection()) {
            String sql = "UPDATE issued_books SET fine = 0 WHERE user_id = ? AND book_id = (SELECT id FROM books WHERE title = ?) AND fine > 0";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, title);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, " Fine paid successfully.");
            } else {
                JOptionPane.showMessageDialog(this, " No pending fine for this book.");
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, " Error: " + e.getMessage());
        e.printStackTrace();
    }

    public static void main(String[] args) {
        new UserGUI("user1"); // Replace with actual username from LoginPage
    }
}
