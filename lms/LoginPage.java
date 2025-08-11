import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LoginPage extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginPage() {
        setTitle("Library Login");
        setSize(400, 250);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel title = new JLabel("Library Management Login", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setBounds(50, 20, 300, 30);
        add(title);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(50, 70, 100, 25);
        add(userLabel);

        usernameField = new JTextField();
        usernameField.setBounds(150, 70, 180, 25);
        add(usernameField);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(50, 110, 100, 25);
        add(passLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(150, 110, 180, 25);
        add(passwordField);

        JButton loginBtn = new JButton("Login");
        loginBtn.setBounds(150, 160, 100, 30);
        add(loginBtn);

        loginBtn.addActionListener(e -> performLogin());

        setVisible(true);
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        try {
            // Connect to database
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/librarydb", "root", "Bittu$123"
            );

            // Check credentials
            String sql = "SELECT role FROM users WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role").toLowerCase();

                JOptionPane.showMessageDialog(this, " Login successful as " + role);
                dispose(); // Close login window

                if (role.equals("admin")) {
                    new LibraryGUI(username); // Admin dashboard
                } else {
                    new UserGUI(username);    // User dashboard
                }

            } else {
                JOptionPane.showMessageDialog(this, " Invalid username or password.");
            }

            conn.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, " Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginPage::new);
    }
}
