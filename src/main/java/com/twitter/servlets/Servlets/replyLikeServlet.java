package com.twitter.servlets.Servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;
import com.twitter.servlets.models.replyLikeModel;

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

public class replyLikeServlet extends HttpServlet {

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
        replyLikeModel likeData = mapper.readValue(request.getReader(), replyLikeModel.class);
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
            int reply_id = likeData.getReplyID();
            if (!checkReply(connection,reply_id)){
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid Reply ID.\"}");
                return;
            }
            if (functions.checkAlreadyLiked(connection, reply_id, user_id, false)) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Already Liked the reply.\"}");
                return;
            }
            String like_type = likeData.getLikeType();
            if (!storeReplyLike(connection, reply_id, user_id, like_type)) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Error 1.\"}");
                return;
            }
            updateLikesInReplyTable(connection, reply_id, like_type, true);
            connection.commit();
            response.setStatus(200);
            PrintWriter out = response.getWriter();
            out.println("{\"Message\":\"Liked the post\"}");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean storeReplyLike(Connection connection, int reply_id, int user_id, String like_type) {
        String storeReplyLikeQuery = "INSERT INTO post_like(user_id,reply_id,like_type) " +
                "VALUES (?,?,?) RETURNING pl_id";
        try (PreparedStatement pt = connection.prepareStatement(storeReplyLikeQuery)) {
            pt.setInt(1, user_id);
            pt.setInt(2, reply_id);
            pt.setString(3, like_type);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void updateLikesInReplyTable(Connection connection, int reply_id, String like_type, boolean flag)
            throws SQLException {
        StringBuilder updateLikeQuery = new StringBuilder();
        if (flag) {
            updateLikeQuery.append("UPDATE replies_table SET likes=likes+1, ");
            like_type += "_em";
            System.out.println(like_type);
            updateLikeQuery.append(like_type).append("=").append(like_type).append("+1");
            updateLikeQuery.append(" WHERE reply_id = ?");
            try (PreparedStatement pt = connection.prepareStatement(updateLikeQuery.toString())) {
                pt.setInt(1, reply_id);
                pt.executeUpdate();
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        } else {
            updateLikeQuery.append("UPDATE replies_table SET likes=likes-1, ");
            like_type += "_em";
            System.out.println(like_type);
            updateLikeQuery.append(like_type).append("=").append(like_type).append("-1");
            updateLikeQuery.append(" WHERE reply_id = ?");
            try (PreparedStatement pt = connection.prepareStatement(updateLikeQuery.toString())) {
                pt.setInt(1, reply_id);
                pt.executeUpdate();
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        }
    }

    private boolean checkReply(Connection connection, int reply_id){
        String checkReplyQuery = "SELECT 1 FROM replies_table WHERE reply_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(checkReplyQuery)){
            pt.setInt(1,reply_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        String authHeader = request.getHeader("Authorization");
        ObjectMapper mapper = new ObjectMapper();
        List<String> cred = functions.getAuthCredentials(authHeader);
        if (cred.isEmpty()) {
            response.setStatus(404);
            PrintWriter out = response.getWriter();
            out.println("{\"Message\":\"Invalid Username and Password\"}");
            return;
        }
        replyLikeModel likeData = mapper.readValue(request.getReader(), replyLikeModel.class);
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            int user_id = functions.retrieveUserid1(connection, cred);
            if (user_id == -1) {
                response.setStatus(404);
                PrintWriter out = response.getWriter();
                out.println("{\"Message\":\"Invalid Username and Password\"}");
                return;
            }
            int reply_id = likeData.getReplyID();
            if (!checkReply(connection, reply_id)) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid Reply ID.\"}");
                return;
            }
            if (!functions.checkAlreadyLiked(connection, reply_id, user_id, false)) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Already unliked the reply.\"}");
                return;
            }
            String like_type = likeData.getLikeType();
            if (like_type == null) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid Like Type.\"}");
                return;
            }
            updateLikesInReplyTable(connection, reply_id, like_type, false);
            if (!deleteReplyLike(connection, reply_id, user_id)) {
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
            out.println("{\"message\":\"Successfully unliked the reply\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"Connection Error\"}");

        }
    }

    private boolean deleteReplyLike(Connection connection, int reply_id, int user_id) {
        String DeletePostLikeQuery = "DELETE FROM post_like WHERE reply_id = ? AND user_id = ? RETURNING pl_id";
        try (PreparedStatement pt = connection.prepareStatement(DeletePostLikeQuery)) {
            pt.setInt(1, reply_id);
            pt.setInt(2, user_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
