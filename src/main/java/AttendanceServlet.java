import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.Duration;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/AttendanceServlet")
public class AttendanceServlet extends HttpServlet {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/EmployeeManagementSystem";

    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "ijustDh53@";


    // =========================
    // POST → Check-in / Check-out (Employee)
    // =========================
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("index.html");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        String action = request.getParameter("action");
        String today = java.time.LocalDate.now().toString();

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            // ========== CHECK-IN ==========
            if ("checkin".equals(action)) {

                String query =
                        "INSERT INTO attendance(user_id,date,check_in,status) " +
                        "VALUES (?, ?, NOW(), 'Present') " +
                        "ON DUPLICATE KEY UPDATE check_in=NOW(), status='Present'";

                PreparedStatement stmt =
                        conn.prepareStatement(query);

                stmt.setInt(1, userId);
                stmt.setString(2, today);

                stmt.executeUpdate();

                out.print(success("Checked in"));

            }

            // ========== CHECK-OUT ==========
            else if ("checkout".equals(action)) {

                calculateWorkHoursAndScore(conn, userId, today);

                String query =
                        "UPDATE attendance SET check_out=NOW() " +
                        "WHERE user_id=? AND date=?";

                PreparedStatement stmt =
                        conn.prepareStatement(query);

                stmt.setInt(1, userId);
                stmt.setString(2, today);

                stmt.executeUpdate();

                out.print(success("Checked out"));

            }

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("DB Error"));

        }
    }


    // =========================
    // GET → Employee + HR View
    // =========================
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
            session.getAttribute("userId") == null) {

            response.sendRedirect("index.html");
            return;
        }

        int userId =
                (int) session.getAttribute("userId");

        String role =
                (String) session.getAttribute("role");

        String date =
                request.getParameter("date");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        JSONArray list = new JSONArray();

        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            PreparedStatement stmt;

            // ================= HR VIEW =================
            if ("HR".equals(role)) {

                String sql =
                        "SELECT a.*,u.name " +
                        "FROM attendance a " +
                        "JOIN users u ON a.user_id=u.id";

                if (date != null && !date.isEmpty()) {
                    sql += " WHERE a.date=?";
                    stmt = conn.prepareStatement(sql);
                    stmt.setString(1, date);
                } else {
                    stmt = conn.prepareStatement(sql);
                }

            }

            // ================= EMPLOYEE VIEW =================
            else {

                String sql =
                        "SELECT date,check_in,check_out,status,work_hours,productivity_score " +
                        "FROM attendance WHERE user_id=?";

                if (date != null && !date.isEmpty()) {
                    sql += " AND date=?";
                    stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, userId);
                    stmt.setString(2, date);
                } else {
                    stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, userId);
                }

            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                JSONObject obj = new JSONObject();

                // HR only
                if ("HR".equals(role)) {
                    obj.put("employee",
                            rs.getString("name"));
                }

                obj.put("date",
                        rs.getString("date"));

                obj.put("check_in",
                        rs.getString("check_in"));

                obj.put("check_out",
                        rs.getString("check_out"));

                obj.put("status",
                        rs.getString("status"));

                obj.put("work_hours",
                        rs.getDouble("work_hours"));

                obj.put("score",
                        rs.getDouble("productivity_score"));

                list.put(obj);
            }

            JSONObject result = new JSONObject();
            result.put("records", list);

            out.print(result);

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("DB Error"));

        }
    }


    // =========================
    // AI Logic
    // =========================
    private void calculateWorkHoursAndScore(
            Connection conn,
            int userId,
            String date) throws Exception {

        String query =
                "SELECT check_in,NOW() AS check_out " +
                "FROM attendance WHERE user_id=? AND date=?";

        PreparedStatement stmt =
                conn.prepareStatement(query);

        stmt.setInt(1, userId);
        stmt.setString(2, date);

        ResultSet rs = stmt.executeQuery();

        if (!rs.next()) return;

        Timestamp in = rs.getTimestamp("check_in");
        Timestamp out = rs.getTimestamp("check_out");

        long minutes =
                Duration.between(
                        in.toLocalDateTime(),
                        out.toLocalDateTime()
                ).toMinutes();

        double hours = minutes / 60.0;

        double score;

        if (hours >= 8) score = 95;
        else if (hours >= 7) score = 85;
        else if (hours >= 6) score = 75;
        else if (hours >= 5) score = 60;
        else score = 40;

        String update =
                "UPDATE attendance SET work_hours=?,productivity_score=? " +
                "WHERE user_id=? AND date=?";

        PreparedStatement ps =
                conn.prepareStatement(update);

        ps.setDouble(1, hours);
        ps.setDouble(2, score);
        ps.setInt(3, userId);
        ps.setString(4, date);

        ps.executeUpdate();
    }


    // =========================
    // JSON Helpers
    // =========================
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
