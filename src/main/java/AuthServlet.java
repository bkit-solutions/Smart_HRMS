import java.io.IOException;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/AuthServlet")
public class AuthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/EmployeeManagementSystem";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "ijustDh53@";

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        String action = request.getParameter("action");

        if ("register".equals(action)) {
            registerUser(request, response);
        } else if ("login".equals(action)) {
            loginUser(request, response);
        } else {
            response.sendRedirect("index.html");
        }
    }

    // 🔹 Register User (With Department Selection)
    private void registerUser(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String role = request.getParameter("role");
        String department = request.getParameter("department");

        // If user is HR, they are always in the HR department
        if ("HR".equals(role)) {
            department = "HR";
        }

        // Hash password before storing
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)) {
            // Check if email already exists
            String checkQuery = "SELECT * FROM users WHERE email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                response.sendRedirect("register.html?error=Email+already+registered!");
                return;
            }

            // Check if HR already exists (Only one HR allowed)
            if ("HR".equals(role)) {
                String hrCheckQuery = "SELECT * FROM users WHERE role = 'HR'";
                PreparedStatement hrCheckStmt = conn.prepareStatement(hrCheckQuery);
                ResultSet hrRs = hrCheckStmt.executeQuery();
                if (hrRs.next()) {
                    response.sendRedirect("register.html?error=HR+already+exists!");
                    return;
                }
            }

            // Insert user into database
            String insertQuery = "INSERT INTO users (name, email, password, role, department) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(insertQuery);
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);
            stmt.setString(4, role);
            stmt.setString(5, department);
            stmt.executeUpdate();

            response.sendRedirect("index.html?success=Registration+successful!+Login+now.");
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("register.html?error=Database+error+occurred.");
        }
    }

    // 🔹 Login User & Redirect Based on Role
    private void loginUser(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)) {
            String query = "SELECT * FROM users WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String hashedPassword = rs.getString("password");
                String role = rs.getString("role");
                String department = rs.getString("department"); // Fetch department

                if (BCrypt.checkpw(password, hashedPassword)) {
                    HttpSession session = request.getSession();
                    session.setAttribute("userId", userId);
                    session.setAttribute("user", email);
                    session.setAttribute("role", role);
                    session.setAttribute("department", department); // Store department in session

                    if ("HR".equals(role)) {
                        response.sendRedirect("HR.html");
                    } else {
                        response.sendRedirect("Employee.html");
                    }
                } else {
                    response.sendRedirect("index.html?error=Invalid+email+or+password.");
                }
            } else {
                response.sendRedirect("index.html?error=Invalid+email+or+password.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("index.html?error=Database+error+occurred.");
        }
    }
}
	