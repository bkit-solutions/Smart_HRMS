import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.json.JSONObject;

@WebServlet("/EmployeeDashboardServlet")
public class EmployeeDashboardServlet extends HttpServlet {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/EmployeeManagementSystem";

    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "ijustDh53@";


    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session =
                request.getSession(false);

        if (session == null ||
                session.getAttribute("userId") == null) {

            response.sendRedirect("index.html");
            return;
        }

        int userId =
                (int) session.getAttribute("userId");

        response.setContentType("application/json");

        PrintWriter out = response.getWriter();

        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            JSONObject result = new JSONObject();

            // ================= USER =================
            String userQ =
                    "SELECT name FROM users WHERE id=?";

            PreparedStatement uStmt =
                    conn.prepareStatement(userQ);

            uStmt.setInt(1, userId);

            ResultSet uRs = uStmt.executeQuery();

            if (uRs.next()) {
                result.put("name",
                        uRs.getString("name"));
            }


            // ================= PERFORMANCE =================
            String perfQ =
                    "SELECT performance_score, " +
                    "performance_grade, risk_level, recommendation " +
                    "FROM performance_reports " +
                    "WHERE user_id=? " +
                    "ORDER BY generated_at DESC LIMIT 1";

            PreparedStatement pStmt =
                    conn.prepareStatement(perfQ);

            pStmt.setInt(1, userId);

            ResultSet pRs = pStmt.executeQuery();

            if (pRs.next()) {

                result.put("score",
                        pRs.getDouble("performance_score"));

                result.put("grade",
                        pRs.getString("performance_grade"));

                result.put("risk",
                        pRs.getString("risk_level"));

                result.put("recommendation",
                        pRs.getString("recommendation"));
            } else {

                result.put("score", "--");
                result.put("grade", "N/A");
                result.put("risk", "N/A");
                result.put("recommendation",
                        "No data yet");
            }


            // ================= ATTENDANCE =================
            String attQ =
                    "SELECT COUNT(*) total, " +
                    "SUM(CASE WHEN status='Present' THEN 1 ELSE 0 END) present " +
                    "FROM attendance " +
                    "WHERE user_id=? " +
                    "AND MONTH(date)=MONTH(CURDATE()) " +
                    "AND YEAR(date)=YEAR(CURDATE())";

            PreparedStatement aStmt =
                    conn.prepareStatement(attQ);

            aStmt.setInt(1, userId);

            ResultSet aRs = aStmt.executeQuery();

            if (aRs.next()) {

                int total = aRs.getInt("total");
                int present = aRs.getInt("present");

                int percent = 0;

                if (total > 0) {
                    percent =
                            (present * 100) / total;
                }

                result.put("attendance", percent);
            }


            // ================= PAYROLL =================
            String payQ =
                    "SELECT net_salary, paid_status " +
                    "FROM payroll " +
                    "WHERE user_id=? " +
                    "ORDER BY year DESC, month DESC LIMIT 1";

            PreparedStatement payStmt =
                    conn.prepareStatement(payQ);

            payStmt.setInt(1, userId);

            ResultSet payRs = payStmt.executeQuery();

            if (payRs.next()) {

                result.put("salary",
                        payRs.getDouble("net_salary"));

                result.put("payStatus",
                        payRs.getString("paid_status"));

            } else {

                result.put("salary", 0);
                result.put("payStatus", "N/A");
            }


            out.print(result);

        } catch (Exception e) {

            e.printStackTrace();

            out.print("{\"success\":false}");
        }
    }
}
