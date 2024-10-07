package com.twitter.servlets.Servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;
import com.twitter.servlets.models.postLikeModel;

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

public class postLikeServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        ObjectMapper mapper = new ObjectMapper();
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if (cred.isEmpty()) {
            response.setStatus(400);
            PrintWriter out = response.getWriter();
            out.println("{\"Message\":\"Invalid Username and Password\"}");
            return;
        }
        postLikeModel likeData = mapper.readValue(request.getReader(), postLikeModel.class);
        try (Connection connection = DBUtil.getConnection()) {
            int post_id = likeData.getPostID();
            String like_type = likeData.getLikeType();
            connection.setAutoCommit(false);
            int user_id = functions.retrieveUserid1(connection, cred);
            if (user_id == -1) {
                connection.rollback();
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            if (!functions.checkPost(connection, post_id)) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid Post ID.\"}");
                return;
            }
            if (functions.checkAlreadyLiked(connection, post_id, user_id, true)) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Already Liked the post.\"}");
                return;
            }
            if (!storePostLike(connection, post_id, user_id, like_type)) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Error 1.\"}");
                return;
            }

            updateLikesinPostTable(connection, post_id, like_type, true);
            connection.commit();
            response.setStatus(200);
            PrintWriter out = response.getWriter();
            out.println("{\"Message\":\"Liked the post\"}");

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(400);
            PrintWriter out = response.getWriter();
            out.println("{\"Message\":\"Query error\"}");
            return;
        }
    }

    private boolean storePostLike(Connection connection, int post_id, int user_id, String like_type)
            throws SQLException {
        String postLikeQuery = "INSERT INTO post_like (user_id, post_id,like_type) VALUES (?,?,?) RETURNING pl_id";
        try (PreparedStatement pt = connection.prepareStatement(postLikeQuery)) {
            pt.setInt(1, user_id);
            pt.setInt(2, post_id);
            pt.setString(3, like_type);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            connection.rollback();
            return false;
        }
    }

    private void updateLikesinPostTable(Connection connection, int post_id, String like_type, boolean flag)
            throws SQLException {
        if (flag) {
            StringBuilder updateLikeQuery = new StringBuilder();
            updateLikeQuery.append("UPDATE post_table SET likes=likes+1, ");
            like_type += "_em";
            System.out.println(like_type);
            updateLikeQuery.append(like_type).append("=").append(like_type).append("+1");
            updateLikeQuery.append(" WHERE post_id = ?");
            try (PreparedStatement pt = connection.prepareStatement(updateLikeQuery.toString())) {
                pt.setInt(1, post_id);
                pt.executeUpdate();
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        } else {
            StringBuilder updateLikeQuery = new StringBuilder();
            updateLikeQuery.append("UPDATE post_table SET likes=likes-1, ");
            like_type += "_em";
            System.out.println(like_type);
            updateLikeQuery.append(like_type).append("=").append(like_type).append("-1");
            updateLikeQuery.append(" WHERE post_id = ?");
            try (PreparedStatement pt = connection.prepareStatement(updateLikeQuery.toString())) {
                pt.setInt(1, post_id);
                pt.executeUpdate();
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        ObjectMapper mapper = new ObjectMapper();
        response.setContentType("application/json");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if (cred.isEmpty()) {
            response.setStatus(400);
            PrintWriter out = response.getWriter();
            out.println("{\"Message\":\"Invalid Username and Password\"}");
            return;
        }
        postLikeModel newData = mapper.readValue(request.getReader(), postLikeModel.class);
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
            int post_id = newData.getPostID();
            if (!functions.checkPost(connection, post_id)) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid Post ID.\"}");
                return;
            }
            if (!functions.checkAlreadyLiked(connection, post_id, user_id, true)) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Already unliked the post.\"}");
                return;
            }
            String like_type = getLikeType(connection, post_id, user_id);
            if (like_type == null) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid Like Type.\"}");
                return;
            }
            updateLikesinPostTable(connection, post_id, like_type, false);

            if (!deletePostLike(connection, post_id, user_id)) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Error 1.\"}");
                return;
            }
            connection.commit();
            response.setStatus(200);
            PrintWriter out = response.getWriter();
            out.println("{\"Message\":\"Unliked the post\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            e.printStackTrace();
            response.setStatus(400);
            PrintWriter out = response.getWriter();
            out.println("{\"Message\":\"Connection error\"}");
        }
    }

    private String getLikeType(Connection connection, int post_id, int user_id) {
        String getLikeTypeQuery = "SELECT like_type FROM post_like where post_id = ? AND user_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(getLikeTypeQuery)) {
            pt.setInt(1, post_id);
            pt.setInt(2, user_id);
            ResultSet rs = pt.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean deletePostLike(Connection connection, int post_id, int user_id) {
        String DeletePostLikeQuery = "DELETE FROM post_like WHERE post_id = ? AND user_id = ? RETURNING pl_id";
        try (PreparedStatement pt = connection.prepareStatement(DeletePostLikeQuery)) {
            pt.setInt(1, post_id);
            pt.setInt(2, user_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
