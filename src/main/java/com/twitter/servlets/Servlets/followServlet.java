package com.twitter.servlets.Servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;
import com.twitter.servlets.models.followModel;

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

public class followServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if(cred.isEmpty()){
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
            return;
        }
        followModel followModel = mapper.readValue(request.getReader(), com.twitter.servlets.models.followModel.class);
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            int follow_id = followModel.getFollow_id();
            int user_id = functions.retrieveUserid1(connection,cred);
            if (user_id == -1) {
                connection.rollback();
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            if (checkAlreadyFollowed(connection, user_id, follow_id)) {
                response.setStatus(400);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\":\"Already Followed.\"}");
                connection.rollback();
                return;
            }
            if (addFollow(connection, user_id, follow_id) &&
                    updateFollowersList(connection, follow_id, true) &&
                    updateFollowingList(connection, user_id, true)) {
                connection.commit();
                response.setStatus(200);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\":\"Successfully Followed the user.\"}");
            } else {
                response.setStatus(400);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\":\"Error.\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(400);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print("{\"message\":\"SQL ERROR.\"}");
        }
    }

    private boolean addFollow(Connection connection, int user_id, int follow_id) throws SQLException {
        String addFollowQuery = "INSERT INTO user_follow (user_id, follow_id) VALUES (?,?)";
        try (PreparedStatement pt = connection.prepareStatement(addFollowQuery)) {
            pt.setInt(1, user_id);
            pt.setInt(2, follow_id);
            pt.executeUpdate();
            return true;
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
            return false;
        }
    }

    private boolean checkAlreadyFollowed(Connection connection, int user_id, int follow_id) {
        String checkAlreadyFollowedQuery = "SELECT 1 FROM user_follow WHERE user_id =? AND follow_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(checkAlreadyFollowedQuery)) {
            pt.setInt(1, user_id);
            pt.setInt(2, follow_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean updateFollowersList(Connection connection, int follow_id, boolean flag)
            throws SQLException {
        if (flag) {
            String updateFollowersQuery = "UPDATE user_add_details SET followers_count=followers_count+1 " +
                    "WHERE user_id = ?";
            try (PreparedStatement pt = connection.prepareStatement(updateFollowersQuery)) {
                pt.setInt(1, follow_id);
                pt.executeUpdate();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
                return false;
            }
        } else {
            String updateFollowersQuery = "UPDATE user_add_details SET followers_count=followers_count-1 " +
                    "WHERE user_id = ?";
            try (PreparedStatement pt = connection.prepareStatement(updateFollowersQuery)) {
                pt.setInt(1, follow_id);
                pt.executeUpdate();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
                return false;
            }
        }
    }

    private boolean updateFollowingList(Connection connection, int user_id, boolean flag)
            throws SQLException {
        if (flag) {
            String updateFollowersQuery = "UPDATE user_add_details SET following_count=following_count+1 " +
                    "WHERE user_id = ?";
            try (PreparedStatement pt = connection.prepareStatement(updateFollowersQuery)) {
                pt.setInt(1, user_id);
                pt.executeUpdate();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
                return false;
            }
        } else {
            String updateFollowersQuery = "UPDATE user_add_details SET following_count=following_count-1 " +
                    "WHERE user_id = ?";
            try (PreparedStatement pt = connection.prepareStatement(updateFollowersQuery)) {
                pt.setInt(1, user_id);
                pt.executeUpdate();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if(cred.isEmpty()){
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
            return;
        }
        followModel followModel = mapper.readValue(request.getReader(),
                com.twitter.servlets.models.followModel.class);
        try (Connection connection = DBUtil.getConnection()) {
            int follow_id = followModel.getFollow_id();
            connection.setAutoCommit(false);
            int user_id = functions.retrieveUserid1(connection,cred);
            if (user_id == -1) {
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            if (!checkAlreadyFollowed(connection, user_id, follow_id)) {
                response.setStatus(400);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\":\"Already Unfollowed.\"}");
                connection.rollback();
                return;
            }
            if (deleteFollow(connection, user_id, follow_id) && updateFollowersList(connection, follow_id, false)
                    && updateFollowingList(connection, user_id, false)) {
                connection.commit();
                response.setStatus(200);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\":\"Successfully Unfollowed the user.\"}");
            } else {
                response.setStatus(400);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\":\"Error.\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(400);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print("{\"message\":\"SQL ERROR.\"}");
        }
    }

    private boolean deleteFollow(Connection connection, int user_id, int follow_id) {
        String deleteFollowQuery = "DELETE FROM user_follow WHERE user_id = ? AND follow_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(deleteFollowQuery)) {
            pt.setInt(1, user_id);
            pt.setInt(2, follow_id);
            pt.executeUpdate();
            return true;
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
        if(cred.isEmpty()){
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
            return;
        }
        StringBuilder jsonResult = new StringBuilder();
        try (Connection connection = DBUtil.getConnection()) {
            int user_id = functions.retrieveUserid1(connection,cred);
            if (user_id == -1) {
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            String FollowListQuery = "SELECT followers_count,following_count FROM user_add_details WHERE user_id = ?";
            PreparedStatement pt = connection.prepareStatement(FollowListQuery);
            pt.setInt(1, user_id);
            ResultSet rs = pt.executeQuery();
            jsonResult.append("[");
            while (rs.next()) {
                jsonResult.append("{");
                jsonResult.append("\"FollowersCount\":\"")
                        .append(rs.getInt("followers_count")).append("\",\n");
                jsonResult.append("\"FollowingCount\":\"")
                        .append(rs.getInt("following_count")).append("\",\n");
                if (jsonResult.length() > 1) {
                    jsonResult.setLength(jsonResult.length() - 2);
                }
                jsonResult.append("},");
            }
            if (jsonResult.length() > 1) {
                jsonResult.setLength(jsonResult.length() - 1);
            }
            jsonResult.append("]");
            response.getWriter().write(jsonResult.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
