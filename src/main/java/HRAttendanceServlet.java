import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/HRAttendanceServlet")
public class HRAttendanceServlet extends HttpServlet {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/EmployeeManagementSystem";

    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "ijustDh53@";

    private static final int PAGE_SIZE = 5;


    // ============================
    // GET → VIEW ATTENDANCE
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

        int page =
                Integer.parseInt(
                        request.getParameter("page") == null ?
                        "1" : request.getParameter("page"));

        String date =
                request.getParameter("date");


        response.setContentType("application/json");
        PrintWriter out = response.getWriter();


        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            int totalRecords =
                    getTotalRecords(conn, date);

            int totalPages =
                    (int) Math.ceil(
                            (double) totalRecords / PAGE_SIZE);


            JSONArray list =
                    fetchAttendance(conn, page, date);


            JSONObject result =
                    new JSONObject();

            result.put("records", list);
            result.put("totalPages", totalPages);

            out.print(result);

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("Load failed"));
        }
    }


    // ============================
    // POST → UPDATE / DELETE / ANALYZE
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

        String action =
                request.getParameter("action");


        response.setContentType("application/json");
        PrintWriter out = response.getWriter();


        try (Connection conn =
                     DriverManager.getConnection(
                             JDBC_URL, JDBC_USER, JDBC_PASS)) {

            switch (action) {

                case "update":
                    updateAttendance(conn, request, out);
                    break;

                case "delete":
                    deleteAttendance(conn, request, out);
                    break;

                case "analyze":
                    analyzeAttendance(conn, out);
                    break;

                default:
                    out.print(error("Invalid action"));
            }

        } catch (Exception e) {

            e.printStackTrace();
            out.print(error("Server error"));
        }
    }


    // ============================
    // FETCH ATTENDANCE (PAGINATED)
    // ============================
    private JSONArray fetchAttendance(Connection conn,
                                      int page,
                                      String date)
            throws Exception {

        JSONArray list = new JSONArray();

        int offset =
                (page - 1) * PAGE_SIZE;


        String query =
                "SELECT a.user_id,u.name,a.date, " +
                "a.check_in,a.check_out,a.status " +
                "FROM attendance a " +
                "JOIN users u ON a.user_id=u.id ";


        if (date != null && !date.isEmpty()) {
            query += "WHERE a.date=? ";
        }

        query += "ORDER BY a.date DESC LIMIT ? OFFSET ?";


        PreparedStatement stmt =
                conn.prepareStatement(query);


        int index = 1;

        if (date != null && !date.isEmpty()) {
            stmt.setString(index++, date);
        }

        stmt.setInt(index++, PAGE_SIZE);
        stmt.setInt(index, offset);


        ResultSet rs = stmt.executeQuery();


        while (rs.next()) {

            JSONObject obj =
                    new JSONObject();

            obj.put("user_id",
                    rs.getInt("user_id"));

            obj.put("employeeName",
                    rs.getString("name"));

            obj.put("date",
                    rs.getString("date"));

            obj.put("check_in",
                    rs.getString("check_in"));

            obj.put("check_out",
                    rs.getString("check_out"));

            obj.put("status",
                    rs.getString("status"));

            list.put(obj);
        }

        return list;
    }


    // ============================
    // COUNT RECORDS
    // ============================
    private int getTotalRecords(Connection conn,
                                String date)
            throws Exception {

        String query =
                "SELECT COUNT(*) FROM attendance";


        if (date != null && !date.isEmpty()) {
            query += " WHERE date=?";
        }

        PreparedStatement stmt =
                conn.prepareStatement(query);


        if (date != null && !date.isEmpty()) {
            stmt.setString(1, date);
        }

        ResultSet rs =
                stmt.executeQuery();

        if (rs.next()) return rs.getInt(1);

        return 0;
    }


    // ============================
    // UPDATE
    // ============================
    private void updateAttendance(Connection conn,
                                  HttpServletRequest req,
                                  PrintWriter out)
            throws Exception {

        int userId =
                Integer.parseInt(
                        req.getParameter("userId"));

        String date =
                req.getParameter("date");

        String checkIn =
                req.getParameter("checkIn");

        String checkOut =
                req.getParameter("checkOut");

        String status =
                req.getParameter("status");


        String query =
                "UPDATE attendance " +
                "SET check_in=?,check_out=?,status=? " +
                "WHERE user_id=? AND date=?";


        PreparedStatement stmt =
                conn.prepareStatement(query);


        stmt.setString(1, checkIn);
        stmt.setString(2, checkOut);
        stmt.setString(3, status);
        stmt.setInt(4, userId);
        stmt.setString(5, date);


        stmt.executeUpdate();

        out.print(success("Updated successfully"));
    }


    // ============================
    // DELETE
    // ============================
    private void deleteAttendance(Connection conn,
                                  HttpServletRequest req,
                                  PrintWriter out)
            throws Exception {

        int userId =
                Integer.parseInt(
                        req.getParameter("userId"));

        String date =
                req.getParameter("date");


        String query =
                "DELETE FROM attendance " +
                "WHERE user_id=? AND date=?";


        PreparedStatement stmt =
                conn.prepareStatement(query);

        stmt.setInt(1, userId);
        stmt.setString(2, date);

        stmt.executeUpdate();

        out.print(success("Deleted successfully"));
    }


    // ============================
    // AI ANALYSIS
    // ============================
    private void analyzeAttendance(Connection conn,
                                   PrintWriter out)
            throws Exception {

        String query =
                "SELECT id,work_hours,productivity_score " +
                "FROM attendance";

        PreparedStatement stmt =
                conn.prepareStatement(query);

        ResultSet rs =
                stmt.executeQuery();

        int count = 0;


        while (rs.next()) {

            int id = rs.getInt("id");

            double hours =
                    rs.getDouble("work_hours");

            double score =
                    rs.getDouble("productivity_score");


            String warning = "Normal";


            if (hours < 5 || score < 50) {
                warning = "Critical";
            }

            else if (hours < 6 || score < 65) {
                warning = "Warning";
            }

            else if (hours < 7) {
                warning = "Monitor";
            }


            String update =
                    "UPDATE attendance " +
                    "SET warning_level=? WHERE id=?";


            PreparedStatement up =
                    conn.prepareStatement(update);

            up.setString(1, warning);
            up.setInt(2, id);

            up.executeUpdate();

            count++;
        }


        JSONObject obj =
                new JSONObject();

        obj.put("success", true);
        obj.put("updated", count);

        out.print(obj);
    }


    // ============================
    // JSON HELPERS
    // ============================
    private String success(String msg) {

        JSONObject obj =
                new JSONObject();

        obj.put("success", true);
        obj.put("message", msg);

        return obj.toString();
    }


    private String error(String msg) {

        JSONObject obj =
                new JSONObject();

        obj.put("success", false);
        obj.put("message", msg);

        return obj.toString();
    }
}
