import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/EmployeePayrollServlet")
public class EmployeePayrollServlet extends HttpServlet {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/EmployeeManagementSystem";

    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "ijustDh53@";


    // ============================
    // GET
    // ============================
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
            session.getAttribute("userId") == null) {

            response.sendRedirect("index.html");
            return;
        }

        int userId = (int) session.getAttribute("userId");

        String action = request.getParameter("action");

        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            if ("view".equals(action)) {

                fetchPayroll(conn, userId, request, response);

            } else if ("downloadPayslip".equals(action)) {

                generatePayslip(conn, userId, request, response);

            } else {

                response.getWriter().print(error("Invalid action"));
            }

        } catch (Exception e) {

            e.printStackTrace();
            response.getWriter().print(error("Server error"));
        }
    }


    // ============================
    // FETCH PAYROLL
    // ============================
    private void fetchPayroll(Connection conn,
                              int userId,
                              HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

        response.setContentType("application/json");

        PrintWriter out = response.getWriter();

        JSONArray records = new JSONArray();


        String yearStr = request.getParameter("year");
        String month = request.getParameter("month");

        int year = Integer.parseInt(yearStr);


        String query =
                "SELECT month, year, salary, bonuses, deductions, " +
                "net_salary, paid_status " +
                "FROM payroll " +
                "WHERE user_id=? AND year=? ";

        if (month != null && !month.isEmpty()) {
            query += "AND month=? ";
        }

        query += "ORDER BY month DESC";


        PreparedStatement stmt =
                conn.prepareStatement(query);

        int index = 1;

        stmt.setInt(index++, userId);
        stmt.setInt(index++, year);

        if (month != null && !month.isEmpty()) {
            stmt.setString(index, month);
        }


        ResultSet rs = stmt.executeQuery();


        while (rs.next()) {

            JSONObject obj = new JSONObject();

            obj.put("month", rs.getString("month"));
            obj.put("year", rs.getInt("year"));

            // ✅ SAME NAMES AS HR
            obj.put("salary", rs.getDouble("salary"));
            obj.put("bonuses", rs.getDouble("bonuses"));
            obj.put("deductions", rs.getDouble("deductions"));
            obj.put("net_salary", rs.getDouble("net_salary"));

            obj.put("paid_status", rs.getString("paid_status"));

            records.put(obj);
        }


        JSONObject result = new JSONObject();
        result.put("records", records);

        out.print(result);
    }


    // ============================
    // PAYSLIP
    // ============================
    private void generatePayslip(Connection conn,
                                 int userId,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
            throws Exception {

        String month = request.getParameter("month");
        int year = Integer.parseInt(request.getParameter("year"));

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();


        String query =
                "SELECT u.name, u.department, " +
                "p.salary, p.bonuses, p.deductions, " +
                "p.net_salary, p.paid_status " +
                "FROM payroll p " +
                "JOIN users u ON p.user_id=u.id " +
                "WHERE p.user_id=? AND p.month=? AND p.year=?";


        PreparedStatement stmt =
                conn.prepareStatement(query);

        stmt.setInt(1, userId);
        stmt.setString(2, month);
        stmt.setInt(3, year);

        ResultSet rs = stmt.executeQuery();


        if (!rs.next()) {
            out.print("<h3>No payroll found</h3>");
            return;
        }

        if (!"Paid".equals(rs.getString("paid_status"))) {
            out.print("<h3>Payslip available only after payment</h3>");
            return;
        }


        String name = rs.getString("name");
        String dept = rs.getString("department");

        double salary = rs.getDouble("salary");
        double bonus = rs.getDouble("bonuses");
        double deduction = rs.getDouble("deductions");
        double net = rs.getDouble("net_salary");


        // ================= HTML =================

        out.println("<html><head>");
        out.println("<title>Payslip</title>");

        out.println("<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css' rel='stylesheet'>");

        out.println("</head><body class='container mt-5'>");

        out.println("<div class='card shadow p-4'>");

        out.println("<h2 class='text-center mb-4'>Salary Payslip</h2>");

        out.println("<table class='table table-bordered'>");

        out.println("<tr><th>Name</th><td>" + name + "</td></tr>");
        out.println("<tr><th>Department</th><td>" + dept + "</td></tr>");
        out.println("<tr><th>Month</th><td>" + month + " " + year + "</td></tr>");

        out.println("</table>");

        out.println("<h5>Earnings</h5>");

        out.println("<table class='table table-striped'>");

        out.println("<tr><td>Salary</td><td>₹" + salary + "</td></tr>");
        out.println("<tr><td>Bonus</td><td>₹" + bonus + "</td></tr>");
        out.println("<tr><td>Deduction</td><td>₹" + deduction + "</td></tr>");

        out.println("<tr class='table-success'>");
        out.println("<td><strong>Net</strong></td>");
        out.println("<td><strong>₹" + net + "</strong></td>");
        out.println("</tr>");

        out.println("</table>");

        out.println("<div class='text-center'>");
        out.println("<button onclick='window.print()' class='btn btn-primary'>Print</button>");
        out.println("</div>");

        out.println("</div></body></html>");
    }


    // ============================
    // ERROR
    // ============================
    private String error(String msg) {

        JSONObject obj = new JSONObject();

        obj.put("success", false);
        obj.put("message", msg);

        return obj.toString();
    }
}
