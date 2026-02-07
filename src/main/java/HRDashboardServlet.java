import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/HRDashboardServlet")
public class HRDashboardServlet extends HttpServlet {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/EmployeeManagementSystem";

    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "ijustDh53@";


    // ============================
    // GET → Dashboard APIs
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

                case "kpi":
                    out.print(getKPI(conn));
                    break;

                case "attendance":
                    out.print(getAttendanceTrend(conn));
                    break;

                case "payroll":
                    out.print(getPayrollStats(conn));
                    break;

                case "leave":
                    out.print(getLeaveStats(conn));
                    break;

                default:
                    out.print(error("Invalid action"));
            }

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("Dashboard error"));
        }
    }


    // ============================
    // KPI SUMMARY
    // ============================
    private String getKPI(Connection conn)
            throws Exception {

        JSONObject result = new JSONObject();


        // Total Employees
        String empQ =
                "SELECT COUNT(*) FROM users WHERE role='Employee'";

        ResultSet ers =
                conn.prepareStatement(empQ).executeQuery();

        int totalEmp = 0;

        if (ers.next()) totalEmp = ers.getInt(1);


        // Present Today
        String presQ =
                "SELECT COUNT(*) FROM attendance " +
                "WHERE date=CURDATE() AND status='Present'";

        ResultSet prs =
                conn.prepareStatement(presQ).executeQuery();

        int present = 0;

        if (prs.next()) present = prs.getInt(1);


        // Pending Leaves
        String leaveQ =
                "SELECT COUNT(*) FROM leave_requests " +
                "WHERE status='Pending'";

        ResultSet lrs =
                conn.prepareStatement(leaveQ).executeQuery();

        int pendingLeaves = 0;

        if (lrs.next()) pendingLeaves = lrs.getInt(1);


        // Total Payroll
        String payQ =
                "SELECT IFNULL(SUM(net_salary),0) FROM payroll";

        ResultSet prs2 =
                conn.prepareStatement(payQ).executeQuery();

        double payroll = 0;

        if (prs2.next()) payroll = prs2.getDouble(1);


        result.put("totalEmployees", totalEmp);
        result.put("presentToday", present);
        result.put("pendingLeaves", pendingLeaves);
        result.put("totalPayroll", payroll);

        return result.toString();
    }


    // ============================
    // ATTENDANCE CHART
    // ============================
    private String getAttendanceTrend(Connection conn)
            throws Exception {

        String query =
                "SELECT date, COUNT(*) total " +
                "FROM attendance " +
                "WHERE status='Present' " +
                "GROUP BY date " +
                "ORDER BY date DESC " +
                "LIMIT 7";

        ResultSet rs =
                conn.prepareStatement(query).executeQuery();

        JSONArray labels = new JSONArray();
        JSONArray values = new JSONArray();

        while (rs.next()) {

            labels.put(rs.getString("date"));
            values.put(rs.getInt("total"));
        }

        JSONObject result = new JSONObject();

        result.put("labels", labels);
        result.put("values", values);

        return result.toString();
    }


    // ============================
    // PAYROLL CHART (FIXED)
    // ============================
    private String getPayrollStats(Connection conn)
            throws Exception {

        String query =
                "SELECT year, month, SUM(net_salary) AS total " +
                "FROM payroll " +
                "GROUP BY year, month " +
                "ORDER BY year DESC, month DESC " +
                "LIMIT 12";

        ResultSet rs =
                conn.prepareStatement(query).executeQuery();

        JSONArray labels = new JSONArray();
        JSONArray values = new JSONArray();

        while (rs.next()) {

            String label =
                    rs.getString("month") + " " + rs.getInt("year");

            labels.put(label);
            values.put(rs.getDouble("total"));
        }

        JSONObject result = new JSONObject();

        result.put("labels", labels);
        result.put("values", values);

        return result.toString();
    }


    // ============================
    // LEAVE TREND
    // ============================
    private String getLeaveStats(Connection conn)
            throws Exception {

        String query =
                "SELECT MONTH(start_date) m, COUNT(*) c " +
                "FROM leave_requests " +
                "GROUP BY m " +
                "ORDER BY m";

        ResultSet rs =
                conn.prepareStatement(query).executeQuery();

        JSONArray labels = new JSONArray();
        JSONArray values = new JSONArray();

        while (rs.next()) {

            labels.put("Month " + rs.getInt("m"));
            values.put(rs.getInt("c"));
        }

        JSONObject result = new JSONObject();

        result.put("labels", labels);
        result.put("values", values);

        return result.toString();
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
