package com.twitter.servlets.Servlets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class tagsServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("application/json");
        ObjectMapper mapper = new ObjectMapper();
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if (cred.isEmpty()) {
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
        }
        JsonNode jsonNode = mapper.readTree(request.getReader());
        String user_tag = jsonNode.get("hash_tag").asText();
        try (Connection connection = DBUtil.getConnection()) {
            int user_id = functions.retrieveUserid1(connection, cred);
            if (user_id == -1) {
                connection.rollback();
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            System.out.println("sample1");
            if (checkTag(connection, user_tag)) {
                System.out.println("sample3");
                connection.close();
                response.setStatus(400);
                PrintWriter out = response.getWriter();
                out.println("{\"Message:\":\"Tag already present.\"}");
                return;
            }
            connection.setAutoCommit(false);
            String NewTagQuery = "INSERT INTO tags (tag_name) VALUES (?)";
            try (PreparedStatement pt = connection.prepareStatement(NewTagQuery)) {
                pt.setString(1, user_tag);
                pt.executeUpdate();
                connection.commit();
                response.setStatus(200);
                PrintWriter out = response.getWriter();
                out.println("{\"Message:\":\"Tag successfully added.\"}");
            } catch (SQLException e) {
                connection.rollback();
                response.setStatus(400);
                PrintWriter out = response.getWriter();
                out.println("{\"Message:\":\"Error with Query.\"}");
                e.printStackTrace();
            }
        } catch (SQLException e) {
            response.setStatus(400);
            PrintWriter out = response.getWriter();
            out.println("{\"Message:\":\"Error with Connection.\"}");
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        response.setContentType("application/json");
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if (cred.isEmpty()) {
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
        }
        StringBuilder jsonResult = new StringBuilder();
        System.out.println("SAMPLE1");
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
            System.out.println("test1");
            String TagsFetchQuery = "SELECT tag_name,used_count,user_follow_count FROM tags";
            PreparedStatement pt = connection.prepareStatement(TagsFetchQuery);
            boolean has_rows = false;
            ResultSet rs = pt.executeQuery();
            connection.commit();
            jsonResult.append("[");
            while (rs.next()) {
                System.out.println("test1");
                has_rows = true;
                jsonResult.append("{\n");
                jsonResult.append("\"hash_tag\":\"")
                        .append(rs.getString("tag_name")).append("\", ");
                jsonResult.append("\"used in posts\":\"")
                        .append(rs.getInt("used_count")).append("\", ");
                jsonResult.append("\"users_follow\":\"")
                        .append(rs.getInt("user_follow_count")).append("\" \n");
                jsonResult.append("},\n");
            }
            if (has_rows) {
                if (jsonResult.length() > 1) {
                    jsonResult.setLength(jsonResult.length() - 2);
                }
                jsonResult.append("]");
                response.setStatus(200);
                response.getWriter().write(jsonResult.toString());
            } else {
                response.setStatus(404);
                jsonResult.append(("\"message\": \"Data not found\"}"));
                response.getWriter().write(jsonResult.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(400);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"Failed to fetch.\"}");
            e.printStackTrace();
        }
    }

    private boolean checkTag(Connection connection, String user_tag) {
        System.out.println("sample2");
        String CheckTagQuery = "SELECT 1 FROM tags WHERE tag_name=?";
        try (PreparedStatement pt = connection.prepareStatement(CheckTagQuery)) {
            pt.setString(1, user_tag);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
