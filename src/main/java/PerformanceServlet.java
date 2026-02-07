import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/PerformanceServlet")
public class PerformanceServlet extends HttpServlet {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/EmployeeManagementSystem";

    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "ijustDh53@";


    // ===========================
    // GET → Fetch Performance
    // ===========================
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);

        if (session == null) {
            out.print(error("Session expired"));
            return;
        }

        String role = (String) session.getAttribute("role");

        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            // ================= EMPLOYEE =================
            if ("Employee".equals(role)) {

                int userId =
                        (int) session.getAttribute("userId");

                out.print(getEmployeeReport(conn, userId));
            }

            // ================= HR =================
            else if ("HR".equals(role)) {

                out.print(getAllReports(conn));
            }

            else {
                out.print(error("Unauthorized"));
            }

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("Server error"));
        }
    }


    // ===========================
    // POST → Add Report (HR)
    // ===========================
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);

        if (session == null ||
            !"HR".equals(session.getAttribute("role"))) {

            out.print(error("Unauthorized"));
            return;
        }


        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            int userId =
                    Integer.parseInt(request.getParameter("userId"));

            int score =
                    Integer.parseInt(request.getParameter("score"));

            String grade =
                    request.getParameter("grade");


            // ===== AUTO RISK + RECOMMEND =====
            String risk;
            String rec;

            if (score >= 80) {
                risk = "Low";
                rec = "Excellent performance. Keep it up!";
            }
            else if (score >= 60) {
                risk = "Medium";
                rec = "Good work. Improve consistency.";
            }
            else {
                risk = "High";
                rec = "Needs improvement. Training required.";
            }


            String sql =
            	    "INSERT INTO performance_reports " +
            	    "(user_id, score, performance_grade, risk_level, recommendation) " +
            	    "VALUES (?,?,?,?,?)";



            PreparedStatement ps =
                    conn.prepareStatement(sql);

            ps.setInt(1, userId);
            ps.setInt(2, score);
            ps.setString(3, grade);
            ps.setString(4, risk);
            ps.setString(5, rec);

            ps.executeUpdate();


            out.print(success("Performance saved"));

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("Save failed"));
        }
    }


    // ===========================
    // EMPLOYEE VIEW
    // ===========================
    private String getEmployeeReport(Connection conn,
                                     int userId)
            throws Exception {

        String sql =
                "SELECT * FROM performance_reports " +
                "WHERE user_id=? " +
                "ORDER BY created_at DESC LIMIT 1";


        PreparedStatement ps =
                conn.prepareStatement(sql);

        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();


        JSONObject obj = new JSONObject();

        if (rs.next()) {

            obj.put("score", rs.getInt("score"));
            obj.put("grade", rs.getString("grade"));
            obj.put("risk", rs.getString("risk"));
            obj.put("recommendation",
                    rs.getString("recommendation"));
        }

        return obj.toString();
    }


    // ===========================
    // HR VIEW
    // ===========================
    private String getAllReports(Connection conn)
            throws Exception {

        JSONArray arr = new JSONArray();

        String sql =
                "SELECT p.*, u.name " +
                "FROM performance_reports p " +
                "JOIN users u ON p.user_id=u.id " +
                "ORDER BY p.created_at DESC";


        ResultSet rs =
                conn.prepareStatement(sql).executeQuery();


        while (rs.next()) {

            JSONObject obj = new JSONObject();

            obj.put("id", rs.getInt("id"));
            obj.put("name", rs.getString("name"));
            obj.put("score", rs.getInt("score"));
            obj.put("grade", rs.getString("grade"));
            obj.put("risk", rs.getString("risk"));
            obj.put("recommendation",
                    rs.getString("recommendation"));

            arr.put(obj);
        }

        return arr.toString();
    }


    // ===========================
    // HELPERS
    // ===========================
    private String success(String msg) {

        JSONObject obj = new JSONObject();

        obj.put("success", true);
        obj.put("message", msg);

        return obj.toString();
    }

    private String error(String msg) {

        JSONObject obj = new JSONObject();

        obj.put("success", false);
        obj.put("message", msg);

        return obj.toString();
    }
}
