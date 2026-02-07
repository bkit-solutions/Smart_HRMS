import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/HRPayrollServlet")
public class HRPayrollServlet extends HttpServlet {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/EmployeeManagementSystem";

    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "ijustDh53@";


    // ============================
    // GET
    // ============================
    @Override
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

            if ("view".equals(action)) {

                out.print(viewPayroll(conn, request));

            } else if ("getEmployees".equals(action)) {

                out.print(getEmployees(conn));

            } else {

                out.print(error("Invalid action"));
            }

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("Payroll error"));
        }
    }


    // ============================
    // POST
    // ============================
    @Override
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

        System.out.println("HRPayroll Action = " + action);

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            switch (action) {

                case "addPayroll":
                    addPayroll(conn, request, out);
                    break;

                case "updatePayroll":
                    updatePayroll(conn, request, out);
                    break;

                case "deletePayroll":
                    deletePayroll(conn, request, out);
                    break;

                case "markAsPaid":
                    markAsPaid(conn, request, out);
                    break;

                default:
                    out.print(error("Invalid action"));
            }

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("Payroll processing error"));
        }
    }


    // ============================
    // ADD PAYROLL (FIXED)
    // ============================
    private void addPayroll(Connection conn,
                            HttpServletRequest request,
                            PrintWriter out)
            throws Exception {

        int userId = Integer.parseInt(request.getParameter("userId"));
        String month = request.getParameter("month");
        int year = Integer.parseInt(request.getParameter("year"));

        double salary =
                Double.parseDouble(request.getParameter("salary"));

        double bonuses =
                Double.parseDouble(request.getParameter("bonuses"));

        double deductions =
                Double.parseDouble(request.getParameter("deductions"));


        // ❌ REMOVED net_salary (DB will calculate it)

        String insert =
                "INSERT INTO payroll " +
                "(user_id, month, year, salary, bonuses, deductions, paid_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'Pending')";

        PreparedStatement stmt =
                conn.prepareStatement(insert);

        stmt.setInt(1, userId);
        stmt.setString(2, month);
        stmt.setInt(3, year);

        stmt.setDouble(4, salary);
        stmt.setDouble(5, bonuses);
        stmt.setDouble(6, deductions);

        stmt.executeUpdate();

        out.print(success("Payroll added successfully"));
    }


    // ============================
    // UPDATE PAYROLL (FIXED)
    // ============================
    private void updatePayroll(Connection conn,
                               HttpServletRequest request,
                               PrintWriter out)
            throws Exception {

        int id =
                Integer.parseInt(request.getParameter("id"));

        double salary =
                Double.parseDouble(request.getParameter("salary"));

        double bonuses =
                Double.parseDouble(request.getParameter("bonuses"));

        double deductions =
                Double.parseDouble(request.getParameter("deductions"));


        // ❌ REMOVED net_salary

        String query =
                "UPDATE payroll " +
                "SET salary=?, bonuses=?, deductions=? " +
                "WHERE id=?";

        PreparedStatement stmt =
                conn.prepareStatement(query);

        stmt.setDouble(1, salary);
        stmt.setDouble(2, bonuses);
        stmt.setDouble(3, deductions);
        stmt.setInt(4, id);

        stmt.executeUpdate();

        out.print(success("Payroll updated"));
    }


    // ============================
    // VIEW
    // ============================
    private String viewPayroll(Connection conn,
                               HttpServletRequest request)
            throws Exception {

        String yearStr = request.getParameter("year");
        String userId = request.getParameter("userId");
        String month = request.getParameter("month");

        int year = Integer.parseInt(yearStr);

        JSONArray list = new JSONArray();

        String query =
                "SELECT p.id, u.name AS employeeName, " +
                "p.month, p.year, p.salary, " +
                "p.bonuses, p.deductions, " +
                "p.net_salary, p.paid_status " +
                "FROM payroll p " +
                "JOIN users u ON p.user_id=u.id " +
                "WHERE p.year=? ";

        if (userId != null && !userId.isEmpty()) {
            query += "AND p.user_id=? ";
        }

        if (month != null && !month.isEmpty()) {
            query += "AND p.month=? ";
        }

        query += "ORDER BY p.month DESC";

        PreparedStatement stmt =
                conn.prepareStatement(query);

        int index = 1;

        stmt.setInt(index++, year);

        if (userId != null && !userId.isEmpty()) {
            stmt.setInt(index++, Integer.parseInt(userId));
        }

        if (month != null && !month.isEmpty()) {
            stmt.setString(index, month);
        }

        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {

            JSONObject obj = new JSONObject();

            obj.put("id", rs.getInt("id"));
            obj.put("employeeName", rs.getString("employeeName"));
            obj.put("month", rs.getString("month"));
            obj.put("year", rs.getInt("year"));

            obj.put("salary", rs.getDouble("salary"));
            obj.put("bonuses", rs.getDouble("bonuses"));
            obj.put("deductions", rs.getDouble("deductions"));

            obj.put("net_salary", rs.getDouble("net_salary"));
            obj.put("paid_status", rs.getString("paid_status"));

            list.put(obj);
        }

        JSONObject result = new JSONObject();
        result.put("records", list);

        return result.toString();
    }


    // ============================
    // EMPLOYEES
    // ============================
    private String getEmployees(Connection conn)
            throws Exception {

        JSONArray list = new JSONArray();

        String query =
                "SELECT id, name FROM users WHERE role='Employee'";

        ResultSet rs =
                conn.prepareStatement(query).executeQuery();

        while (rs.next()) {

            JSONObject obj = new JSONObject();

            obj.put("id", rs.getInt("id"));
            obj.put("name", rs.getString("name"));

            list.put(obj);
        }

        return list.toString();
    }


    // ============================
    // PAY
    // ============================
    private void markAsPaid(Connection conn,
                            HttpServletRequest request,
                            PrintWriter out)
            throws Exception {

        int id =
                Integer.parseInt(request.getParameter("id"));

        String query =
                "UPDATE payroll SET paid_status='Paid' WHERE id=?";

        PreparedStatement stmt =
                conn.prepareStatement(query);

        stmt.setInt(1, id);

        stmt.executeUpdate();

        out.print(success("Marked as paid"));
    }


    // ============================
    // DELETE
    // ============================
    private void deletePayroll(Connection conn,
                               HttpServletRequest request,
                               PrintWriter out)
            throws Exception {

        int id =
                Integer.parseInt(request.getParameter("id"));

        String query =
                "DELETE FROM payroll WHERE id=?";

        PreparedStatement stmt =
                conn.prepareStatement(query);

        stmt.setInt(1, id);

        stmt.executeUpdate();

        out.print(success("Payroll deleted"));
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
