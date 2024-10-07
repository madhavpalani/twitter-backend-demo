package com.twitter.servlets.Servlets;

import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class paymentServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if (cred.isEmpty()) {
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
            return;
        }
        try (Connection connection = DBUtil.getConnection()) {
            int user_id = functions.retrieveUserid1(connection, cred);
            if (user_id == -1) {
                response.setStatus(404);
                PrintWriter out = response.getWriter();
                out.println("{\"Message:\":\"Invalid username or password.\"}");
                return;
            }
            if ((checkUser(connection, user_id))) {
                response.setStatus(404);
                PrintWriter out = response.getWriter();
                out.println("{\"Message:\":\"User not found.\"}");
                return;
            }
            if ((checkAlreadySubscribed(connection, user_id))) {
                response.setStatus(400);
                PrintWriter out = response.getWriter();
                out.println("{\"Message\":\"User Already Subscribed.\"}");
                return;
            }
            String PaymentCreateQuery = "INSERT INTO payment_table (user_id, paid_date, valid_upto) " +
                    "VALUES (?,?,?) RETURNING user_id";
            connection.setAutoCommit(false);
            try (PreparedStatement pt = connection.prepareStatement(PaymentCreateQuery)) {
                pt.setInt(1, user_id);
                LocalDate paidDate = LocalDate.now();
                pt.setDate(2, Date.valueOf(paidDate));
                LocalDate validDate = paidDate.plusDays(30);
                pt.setDate(3, Date.valueOf(validDate));
                ResultSet rs = pt.executeQuery();
                int return_id;
                if (rs.next()) {
                    return_id = rs.getInt("user_id");
                } else {
                    connection.rollback();
                    response.setStatus(400);
                    PrintWriter out = response.getWriter();
                    out.println("{\"Message:\":\"Error with update.\"}");
                    return;
                }
                boolean status = updateUserDetailsForVerified(connection, return_id);
                if (status) {
                    connection.commit();
                    response.setStatus(201);
                    PrintWriter out = response.getWriter();
                    out.println(("{\"Message\":\"Successfully added.\"}"));
                } else {
                    connection.rollback();
                    response.setStatus(400);
                    PrintWriter out = response.getWriter();
                    out.println("{\"Message:\":\"Error with update.\"}");
                }
            } catch (SQLException e) {
                connection.rollback();
                response.setStatus(400);
                PrintWriter out = response.getWriter();
                out.println("{\"Message:\":\"Query error.\"}");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(400);
            PrintWriter out = response.getWriter();
            out.println("{\"Message:\":\"Connection error.\"}");
        }
    }


    private boolean checkUser(Connection connection, int user_id) {
        String CheckUserQuery = "SELECT 1 FROM payment_table WHERE user_id =?";
        try (PreparedStatement pt = connection.prepareStatement(CheckUserQuery)) {
            pt.setInt(1, user_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean updateUserDetailsForVerified(Connection connection, int user_id) {
        String updateVerifiedQuery = "UPDATE user_details SET is_verified=true WHERE user_id =?";
        try (PreparedStatement pt = connection.prepareStatement(updateVerifiedQuery)) {
            pt.setInt(1, user_id);
            pt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean checkAlreadySubscribed(Connection connection, int user_id) {
        String checkAlreadySubQuery = "SELECT 1 FROM payment_table WHERE user_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(checkAlreadySubQuery)) {
            pt.setInt(1, user_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        StringBuilder jsonResult = new StringBuilder();
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if (cred.isEmpty()) {
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
            return;
        }
        LocalDate currDate = LocalDate.now();
        String FetchDurationQuery = "SELECT valid_upto - ? AS validity FROM payment_table WHERE user_id=?";
        try (Connection connection = DBUtil.getConnection()) {
            int user_id = functions.retrieveUserid1(connection, cred);
            if (user_id == -1) {
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            PreparedStatement pt = connection.prepareStatement(FetchDurationQuery);
            pt.setDate(1, Date.valueOf(currDate));
            pt.setInt(2, user_id);
            ResultSet rs = pt.executeQuery();
            boolean hasRows = false;
            jsonResult.append("[");
            jsonResult.append("{\n");
            while (rs.next()) {
                hasRows = true;
                jsonResult.append("\"Validity\":\"").append(rs.getInt(1)).append("\"}\n");
            }
            if (hasRows) {
                if (jsonResult.length() > 1) {
                    jsonResult.setLength(jsonResult.length() - 1);
                }
                jsonResult.append("]");
                response.setStatus(200);
                response.getWriter().write(jsonResult.toString());
            } else {
                response.setStatus(404);
                jsonResult.append(("\"message\": \"Not Subscribed\"}"));
                jsonResult.append("]");
                response.getWriter().write(jsonResult.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(404);
            PrintWriter out = response.getWriter();
            System.out.println("test1");
            out.println("{\"Message:\":\"Error.\"}");
        }
    }
}
