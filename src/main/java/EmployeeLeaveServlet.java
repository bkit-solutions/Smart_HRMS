import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/EmployeeLeaveServlet")
public class EmployeeLeaveServlet extends HttpServlet {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/EmployeeManagementSystem";

    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "ijustDh53@";


    // ======================
    // GET → History + Stats
    // ======================
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

        fetchLeaveData(response, userId);
    }


    // ======================
    // POST → Apply / Cancel
    // ======================
    protected void doPost(HttpServletRequest request,
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

        String action =
                request.getParameter("action");

        if ("applyLeave".equals(action)) {
            applyLeave(request, response, userId);
        }

        else if ("cancelLeave".equals(action)) {
            cancelLeave(request, response, userId);
        }
    }


    // ======================
    // Fetch History + Stats
    // ======================
    private void fetchLeaveData(HttpServletResponse response,
                                int userId)
            throws IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        JSONArray records = new JSONArray();

        int total = 0;
        int approved = 0;
        int pending = 0;
        int rejected = 0;

        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            String sql =
                    "SELECT id,leave_type,start_date,end_date,reason,status " +
                    "FROM leave_requests WHERE user_id=? " +
                    "ORDER BY start_date DESC";

            PreparedStatement stmt =
                    conn.prepareStatement(sql);

            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                JSONObject obj = new JSONObject();

                String status =
                        rs.getString("status");

                total++;

                if ("Approved".equals(status)) approved++;
                else if ("Pending".equals(status)) pending++;
                else if ("Rejected".equals(status)) rejected++;

                obj.put("id", rs.getInt("id"));
                obj.put("leave_type", rs.getString("leave_type"));
                obj.put("start_date", rs.getString("start_date"));
                obj.put("end_date", rs.getString("end_date"));
                obj.put("reason", rs.getString("reason"));
                obj.put("status", status);

                records.put(obj);
            }

            JSONObject result = new JSONObject();

            result.put("records", records);

            // Stats
            result.put("total", total);
            result.put("approved", approved);
            result.put("pending", pending);
            result.put("rejected", rejected);

            out.print(result);

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("Database error"));

        }
    }


    // ======================
    // Apply Leave
    // ======================
    private void applyLeave(HttpServletRequest request,
                            HttpServletResponse response,
                            int userId)
            throws IOException {

        String type =
                request.getParameter("leaveType");

        String start =
                request.getParameter("startDate");

        String end =
                request.getParameter("endDate");

        String reason =
                request.getParameter("reason");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        if (start.compareTo(end) > 0) {
            out.print(error("Invalid date range"));
            return;
        }

        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            // Proper overlap check
            String check =
                    "SELECT COUNT(*) FROM leave_requests " +
                    "WHERE user_id=? AND NOT (end_date < ? OR start_date > ?)";

            PreparedStatement ps =
                    conn.prepareStatement(check);

            ps.setInt(1, userId);
            ps.setString(2, start);
            ps.setString(3, end);

            ResultSet rs = ps.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {

                out.print(error("Leave already exists"));
                return;
            }

            String insert =
                    "INSERT INTO leave_requests " +
                    "(user_id,leave_type,start_date,end_date,reason,status) " +
                    "VALUES (?,?,?,?,?,'Pending')";

            PreparedStatement stmt =
                    conn.prepareStatement(insert);

            stmt.setInt(1, userId);
            stmt.setString(2, type);
            stmt.setString(3, start);
            stmt.setString(4, end);
            stmt.setString(5, reason);

            stmt.executeUpdate();

            out.print(success("Leave applied"));

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("Database error"));

        }
    }


    // ======================
    // Cancel Leave
    // ======================
    private void cancelLeave(HttpServletRequest request,
                             HttpServletResponse response,
                             int userId)
            throws IOException {

        int leaveId =
                Integer.parseInt(request.getParameter("id"));

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            String check =
                    "SELECT status FROM leave_requests " +
                    "WHERE id=? AND user_id=?";

            PreparedStatement ps =
                    conn.prepareStatement(check);

            ps.setInt(1, leaveId);
            ps.setInt(2, userId);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                out.print(error("Not found"));
                return;
            }

            if (!"Pending".equals(rs.getString("status"))) {
                out.print(error("Only pending can cancel"));
                return;
            }

            String del =
                    "DELETE FROM leave_requests " +
                    "WHERE id=? AND user_id=?";

            PreparedStatement stmt =
                    conn.prepareStatement(del);

            stmt.setInt(1, leaveId);
            stmt.setInt(2, userId);

            stmt.executeUpdate();

            out.print(success("Leave cancelled"));

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("Database error"));

        }
    }


    // ======================
    // JSON Helpers
    // ======================
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
