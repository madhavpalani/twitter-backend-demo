package com.twitter.servlets.Servlets;

import javax.servlet.http.HttpServlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;
import com.twitter.servlets.models.delegationModel;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class delegationServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        ObjectMapper mapper = new ObjectMapper();
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if (cred.isEmpty()) {
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
            return;
        }
        delegationModel input = mapper.readValue(request.getReader(), delegationModel.class);
        int delegated_id = input.getD_id();
        System.out.println(delegated_id);
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            int user_id = functions.retrieveUserid1(connection, cred);
            if (user_id == -1) {
                connection.rollback();
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            if ((checkUserid(connection, user_id)) && (checkUserid(connection, delegated_id)) && !checkAlreadyDelegated(connection, user_id, delegated_id)) {
                String DelegatedAddQuery = "INSERT INTO delegation_table (user_id,delegated_id) VALUES (?,?)";
                try (PreparedStatement pt = connection.prepareStatement(DelegatedAddQuery)) {
                    pt.setInt(1, user_id);
                    pt.setInt(2, delegated_id);
                    pt.executeUpdate();
                    connection.commit();
                    response.setStatus(200);
                    PrintWriter out = response.getWriter();
                    out.println("{\"Message:\":\"Successfully added.\"}");
                } catch (SQLException e) {
                    connection.rollback();
                    response.setStatus(400);
                    PrintWriter out = response.getWriter();
                    out.println("{\"Message:\":\"Error with Query.\"}");
                    e.printStackTrace();
                }

            } else {
                connection.rollback();
                response.setStatus(404);
                PrintWriter out = response.getWriter();
                out.println("{\"Message:\":\"User ID not found.\"}");
            }
        } catch (SQLException e) {
            response.setStatus(404);
            PrintWriter out = response.getWriter();
            out.println("{\"Message:\":\"Error found 1.\"}");
            e.printStackTrace();
        }
    }

    private boolean checkAlreadyDelegated(Connection connection, int user_id, int del_id){
        String CheckAlreadyDelQuery = "SELECT 1 FROM delegation_table WHERE user_id = ? AND delegated_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(CheckAlreadyDelQuery)){
            pt.setInt(1,user_id);
            pt.setInt(2,del_id);
            ResultSet rs  = pt.executeQuery();
            return rs.next();
        } catch (SQLException e){
            e.printStackTrace();
            return true;
        }
    }
    private boolean checkUserid(Connection connection, int user_id) {
        String CheckUserIdQuery = "SELECT 1 FROM user_details WHERE user_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(CheckUserIdQuery)) {
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
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            StringBuilder jsonResult = new StringBuilder();
            String GetDelegatedCount = "SELECT delegated_id FROM delegation_table WHERE user_id = ?";
            PreparedStatement pt = connection.prepareStatement(GetDelegatedCount);
            pt.setInt(1, user_id);
            ResultSet rs = pt.executeQuery();
            int x = 1;
            boolean has_rows = false;
            jsonResult.append("{");
            while (rs.next()) {
                has_rows = true;
                jsonResult.append("\"Delegated_account ").append(x++).append("\":\"").
                        append(rs.getInt("delegated_id")).append("\",\n");
            }
            if (has_rows) {
                if (jsonResult.length() > 1) {
                    jsonResult.setLength(jsonResult.length() - 2);
                    jsonResult.append("}\n");
                }
                response.setStatus(200);
                response.getWriter().write(jsonResult.toString());
            } else {
                response.setStatus(404);
                jsonResult.append(("\"message\": \"Data not found\"}"));
                response.getWriter().write(jsonResult.toString());

            }


        } catch (SQLException e) {
            response.setStatus(404);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"Failed to fetch.\"}");
            e.printStackTrace();

        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if (cred.isEmpty()) {
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        delegationModel input = mapper.readValue(request.getReader(), delegationModel.class);
        int delegated_id = input.getD_id();
        System.out.println(delegated_id);
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            int user_id = functions.retrieveUserid1(connection, cred);
            if (user_id == -1) {
                connection.rollback();
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            if(!checkUserid(connection,delegated_id)){
                connection.rollback();
                response.setStatus(404);
                PrintWriter out = response.getWriter();
                out.println("{\"Message:\":\"Wrong delegated id.\"}");
                return;
            }
            String DeleteDelegateQuery = "DELETE FROM delegation_table WHERE user_id=? AND delegated_id=?";
            try (PreparedStatement pt = connection.prepareStatement(DeleteDelegateQuery)) {
                pt.setInt(1, user_id);
                pt.setInt(2, delegated_id);
                int rowsAffected = pt.executeUpdate();
                if (rowsAffected == 0) {
                    connection.close();
                    response.setStatus(404);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Data not found.\"}");
                    return;
                }

            } catch (SQLException e) {
                e.printStackTrace();
                connection.rollback();
                response.setStatus(400);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\":\"Error1.\"}");
                return;
            }
            connection.commit();
            response.setStatus(200);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print("{\"message\":\"Successfully deleted.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(400);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print("{\"message\":\"Error2.\"}");
        }
    }
}
