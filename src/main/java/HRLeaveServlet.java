import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/HRLeaveServlet")
public class HRLeaveServlet extends HttpServlet {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/EmployeeManagementSystem";

    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "ijustDh53@";


    // ============================
    // GET → View / Employees
    // ============================
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
                !"HR".equals(session.getAttribute("role"))) {

            response.sendRedirect("index.html");
            return;
        }

        String action = request.getParameter("action");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            switch (action) {

                case "getEmployees":
                    out.print(getEmployees(conn));
                    break;

                case "viewLeaveRequests":
                    out.print(viewLeaves(conn, request));
                    break;

                default:
                    out.print(error("Invalid action"));
            }

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("Leave system error"));
        }
    }


    // ============================
    // POST → Approve / Reject
    // ============================
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
                !"HR".equals(session.getAttribute("role"))) {

            response.sendRedirect("index.html");
            return;
        }

        String action = request.getParameter("action");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            if ("updateLeaveStatus".equals(action)) {

                updateStatus(conn, request, out);

            } else {

                out.print(error("Invalid action"));
            }

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("Leave processing error"));
        }
    }


    // ============================
    // VIEW LEAVES
    // ============================
    private String viewLeaves(Connection conn,
                              HttpServletRequest request)
            throws Exception {

        String empId = request.getParameter("employeeId");
        String status = request.getParameter("status");

        JSONArray list = new JSONArray();

        String query =
                "SELECT l.id, u.name AS employee_name, l.leave_type, " +
                "l.start_date, l.end_date, l.reason, l.status " +
                "FROM leave_requests l " +
                "JOIN users u ON l.user_id=u.id " +
                "WHERE 1=1";

        if (empId != null && !empId.isEmpty()) {
            query += " AND l.user_id=?";
        }

        if (status != null && !status.isEmpty()) {
            query += " AND l.status=?";
        }

        query += " ORDER BY l.start_date DESC";

        PreparedStatement stmt =
                conn.prepareStatement(query);

        int index = 1;

        if (empId != null && !empId.isEmpty()) {
            stmt.setInt(index++, Integer.parseInt(empId));
        }

        if (status != null && !status.isEmpty()) {
            stmt.setString(index, status);
        }

        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {

            JSONObject obj = new JSONObject();

            obj.put("id", rs.getInt("id"));
            obj.put("employee_name", rs.getString("employee_name"));

            obj.put("leave_type", rs.getString("leave_type"));

            obj.put("start_date", rs.getString("start_date"));
            obj.put("end_date", rs.getString("end_date"));

            obj.put("reason", rs.getString("reason"));
            obj.put("status", rs.getString("status"));

            list.put(obj);
        }

        JSONObject result = new JSONObject();
        result.put("records", list);

        return result.toString();
    }


    // ============================
    // UPDATE STATUS
    // ============================
    private void updateStatus(Connection conn,
                              HttpServletRequest request,
                              PrintWriter out)
            throws Exception {

        int leaveId =
                Integer.parseInt(request.getParameter("id"));

        String status =
                request.getParameter("status");


        String query =
                "UPDATE leave_requests " +
                "SET status=? " +
                "WHERE id=?";

        PreparedStatement stmt =
                conn.prepareStatement(query);

        stmt.setString(1, status);
        stmt.setInt(2, leaveId);

        stmt.executeUpdate();

        out.print(success("Leave status updated"));
    }


    // ============================
    // EMPLOYEES
    // ============================
    private String getEmployees(Connection conn)
            throws Exception {

        JSONArray list = new JSONArray();

        String query =
                "SELECT id, name FROM users WHERE role='Employee'";

        PreparedStatement stmt =
                conn.prepareStatement(query);

        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {

            JSONObject obj = new JSONObject();

            obj.put("id", rs.getInt("id"));
            obj.put("name", rs.getString("name"));

            list.put(obj);
        }

        return list.toString();
    }


    // ============================
    // HELPERS
    // ============================
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
