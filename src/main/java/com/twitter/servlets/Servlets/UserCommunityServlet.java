package com.twitter.servlets.Servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;
import com.twitter.servlets.models.UserCommunityModel;

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

public class UserCommunityServlet extends HttpServlet {
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
        UserCommunityModel userCommunityModel = mapper.readValue(request.getReader(), UserCommunityModel.class);
        int communityId = userCommunityModel.getCommunityId();
        String position = userCommunityModel.getPosition();

        try (Connection connection = DBUtil.getConnection()) {
            int user_id = functions.retrieveUserid1(connection,cred);
            if (user_id == -1) {
                connection.rollback();
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            if (checkAlreadyFollowed(connection, user_id, communityId)) {
                response.setStatus(400);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.println("{\"message\": \"Already Followed.\"}");
                return;
            }
            if (!(checkValidCommunityId(connection, communityId))) {
                response.setStatus(404);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.println("{\"message\": \"Invalid Community.\"}");
                return;
            }
            connection.setAutoCommit(false);
            int cu_id = insertCommunityUser(connection, user_id, communityId, position);
            if (cu_id < 0) {
                response.setStatus(400);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.println("{\"message\": \"Error with Insert Query.\"}");
                return;
            }
            if (updateCommunityFollow(connection, communityId, true)) {
                response.setStatus(400);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.println("{\"message\": \"Error with update.\"}");
                return;
            }
            updateUserFollowList(connection, user_id, true);
            connection.commit();
            response.setStatus(200);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.println("{\"message\": \"Successfully followed.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(400);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.println("{\"message\": \"Catch Error.\"}");
        }
    }

    private boolean checkAlreadyFollowed(Connection connection, int user_id, int c_id) {
        String CheckVerifiedQuery = "SELECT 1 FROM community_user WHERE user_id = ? AND c_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(CheckVerifiedQuery)) {
            pt.setInt(1, user_id);
            pt.setInt(2, c_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean checkValidUserId(Connection connection, int user_id) {
        String CheckVerifiedQuery = "SELECT 1 FROM user_details WHERE user_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(CheckVerifiedQuery)) {
            pt.setInt(1, user_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean checkValidCommunityId(Connection connection, int c_id) {
        String CheckVerifiedQuery = "SELECT 1 FROM community_table WHERE c_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(CheckVerifiedQuery)) {
            pt.setInt(1, c_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void updateUserFollowList(Connection connection, int user_id, boolean flag) {
        if (flag) {
            String userFollowQuery = "UPDATE user_add_details SET communities_in=communities_in+1 " +
                    "WHERE user_id=?";
            try (PreparedStatement pt = connection.prepareStatement(userFollowQuery)) {
                pt.setInt(1, user_id);
                pt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            String userFollowQuery = "UPDATE user_add_details SET communities_in=communities_in-1 " +
                    "WHERE user_id=?";
            try (PreparedStatement pt = connection.prepareStatement(userFollowQuery)) {
                pt.setInt(1, user_id);
                pt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private int insertCommunityUser(Connection connection, int user_id, int c_id, String role) throws SQLException {
        String insertCommunityUserQuery = "INSERT INTO community_user(user_id,c_id,role_name) " +
                "VALUES (?,?,?) RETURNING cu_id";
        try (PreparedStatement pt = connection.prepareStatement(insertCommunityUserQuery)) {
            pt.setInt(1, user_id);
            pt.setInt(2, c_id);
            pt.setString(3, role);
            ResultSet rs = pt.executeQuery();
            if (rs.next()) {
                return rs.getInt("cu_id");
            }
            return -1;
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
            return -1;
        }
    }

    private boolean updateCommunityFollow(Connection connection, int c_id, boolean flag) throws SQLException {
        if (flag) {
            String updateCommunityFollowQuery = "UPDATE community_table SET user_count = user_count+1 " +
                    "WHERE c_id =?";
            try (PreparedStatement pt = connection.prepareStatement(updateCommunityFollowQuery)) {
                pt.setInt(1, c_id);
                pt.executeUpdate();
                return false;
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
                return true;
            }
        } else {
            String updateCommunityFollowQuery = "UPDATE community_table SET user_count = user_count-1 " +
                    "WHERE c_id =?";
            try (PreparedStatement pt = connection.prepareStatement(updateCommunityFollowQuery)) {
                pt.setInt(1, c_id);
                pt.executeUpdate();
                return false;
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
                return true;
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
        }
        UserCommunityModel userCommunityModel = mapper.readValue(request.getReader(), UserCommunityModel.class);
        int communityId = userCommunityModel.getCommunityId();
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            int user_id  = functions.retrieveUserid1(connection,cred);
            if (user_id == -1) {
                connection.rollback();
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            if (!checkAlreadyFollowed(connection, user_id, communityId)) {
                response.setStatus(404);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.println("{\"message\": \"Already Unfollowed.\"}");
                return;
            }
            if (!DeleteCommunityUser(connection, communityId, user_id)) {
                response.setStatus(404);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.println("{\"message\": \"Error with Query1.\"}");
                return;
            }
            if (updateCommunityFollow(connection, communityId, false)) {
                response.setStatus(404);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.println("{\"message\": \"Error with Query2.\"}");
                return;
            }
            updateUserFollowList(connection, user_id, false);
            connection.commit();
            response.setStatus(200);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.println("{\"message\": \"Successfully unfollowed\"}");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean DeleteCommunityUser(Connection connection, int c_id, int user_id) throws SQLException {
        String DeleteCommUserQuery = "DELETE FROM community_user WHERE c_id = ? AND user_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(DeleteCommUserQuery)) {
            pt.setInt(1, c_id);
            pt.setInt(2, user_id);
            pt.executeUpdate();
            return true;
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
            return false;
        }
    }
}
