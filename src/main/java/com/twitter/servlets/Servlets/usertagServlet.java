package com.twitter.servlets.Servlets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
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

@WebServlet("/usertags/*")
public class usertagServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if (cred.isEmpty()) {
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(request.getReader());
        String tag_name = jsonNode.get("hashtag").asText();
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
            if (!checkUser(connection, user_id)) {
                System.out.println(checkUser(connection, user_id));
                response.setStatus(404);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\":\"User id not found.\"}");
                return;
            }
            int ut_id;
            int tag_id = fetchTagIDfromName(connection, tag_name);
            if (tag_id != -1) {
                if ((checkTagsFollow(connection, tag_id, user_id))) {
                    response.setStatus(400);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Already following.\"}");
                    return;
                }
                ut_id = addUserTag(connection, tag_id, user_id);
                if (ut_id > 0) {
                    updateTagsFollow(connection, tag_id, 1);
                    updateTagsFollowedByUser(connection, user_id, true);
                    connection.commit();
                    response.setStatus(200);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Successfully given a follow.\"}");
                } else {
                    connection.rollback();
                    response.setStatus(400);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Error while Updating.\"}");
                }
            } else {
                connection.rollback();
                response.setStatus(400);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\":\"Tag not found.\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int fetchTagIDfromName(Connection connection, String tag_name) {
        String fetchTagIdQuery = "SELECT tag_id from tags WHERE tag_name = ?";
        try (PreparedStatement pt = connection.prepareStatement(fetchTagIdQuery)) {
            pt.setString(1, tag_name);
            ResultSet rs = pt.executeQuery();
            if (rs.next()) {
                return rs.getInt("tag_id");
            } else {
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private int addUserTag(Connection connection, int tag_id, int user_id) throws SQLException {
        String addUserTagQuery = "INSERT INTO user_tags (user_id,tag_id) VALUES (?,?) RETURNING ut_id";
        try (PreparedStatement pt = connection.prepareStatement(addUserTagQuery)) {
            pt.setInt(1, user_id);
            pt.setInt(2, tag_id);
            ResultSet rs = pt.executeQuery();
            if (rs.next()) {
                return rs.getInt("ut_id");
            } else {
                return -1;
            }
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
            return -1;
        }
    }

    private void updateTagsFollow(Connection connection, int tag_id, int flag) throws SQLException {
        if (flag == 1) {
            String UpdateTagsFollow = "UPDATE tags SET user_follow_count=user_follow_count+1 WHERE tag_id = ?";
            try (PreparedStatement pt = connection.prepareStatement(UpdateTagsFollow)) {
                pt.setInt(1, tag_id);
                pt.executeUpdate();

            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        } else {
            String UpdateTagsFollow = "UPDATE tags SET user_follow_count=user_follow_count-1 WHERE tag_id = ?";
            try (PreparedStatement pt = connection.prepareStatement(UpdateTagsFollow)) {
                pt.setInt(1, tag_id);
                pt.executeUpdate();

            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        }
    }

    private boolean checkUser(Connection connection, int user_id) {
        String CheckUserQuery = "SELECT 1 FROM user_details WHERE user_id =?";
        try (PreparedStatement pt = connection.prepareStatement(CheckUserQuery)) {
            pt.setInt(1, user_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean checkTagsFollow(Connection connection, int tag_id, int user_id) {
        String checkTagsFollowQuery = "SELECT 1 FROM user_tags WHERE user_id = ? AND tag_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(checkTagsFollowQuery)) {
            pt.setInt(1, user_id);
            pt.setInt(2, tag_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if (cred.isEmpty()) {
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(request.getReader());
        String tag_name = jsonNode.get("hashtag").asText();
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
            if (!checkUser(connection, user_id)) {
                System.out.println(checkUser(connection, user_id));
                response.setStatus(404);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\":\"User id not found.\"}");
                return;
            }
            int ut_id;
            int tag_id = fetchTagIDfromName(connection, tag_name);
            if (tag_id != -1) {
                if (!(checkTagsFollow(connection, tag_id, user_id))) {
                    response.setStatus(404);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Already not following.\"}");
                    return;
                }
                ut_id = DeleteUserTag(connection, user_id, tag_id);
                if (ut_id > 0) {
                    updateTagsFollow(connection, tag_id, 0);
                    updateTagsFollowedByUser(connection, user_id, false);
                    connection.commit();
                    response.setStatus(200);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message:\":\"Successfully unfollowed.\"}");
                } else {
                    connection.rollback();
                    response.setStatus(400);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Data not found in user-tags.\"}");
                }
            } else {
                connection.rollback();
                response.setStatus(400);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\":\"Tag not found.\"}");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int DeleteUserTag(Connection connection, int user_id, int tag_id) {
        String DeleteUserTagQuery = "DELETE FROM user_tags WHERE user_id = ? AND tag_id = ? " +
                "RETURNING ut_id";
        try (PreparedStatement pt = connection.prepareStatement(DeleteUserTagQuery)) {
            pt.setInt(1, user_id);
            pt.setInt(2, tag_id);
            ResultSet rs = pt.executeQuery();
            if (rs.next()) {
                return rs.getInt("ut_id");
            }
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void updateTagsFollowedByUser(Connection connection, int user_id, boolean flag) {
        if (flag) {
            String updateTagsFollowedByUserQuery = "UPDATE user_add_details SET tags_followed = tags_followed+1 " +
                    "WHERE user_id = ?";
            try (PreparedStatement pt = connection.prepareStatement(updateTagsFollowedByUserQuery)) {
                pt.setInt(1, user_id);
                pt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            String updateTagsFollowedByUserQuery = "UPDATE user_add_details SET tags_followed = tags_followed-1 " +
                    "WHERE user_id = ?";
            try (PreparedStatement pt = connection.prepareStatement(updateTagsFollowedByUserQuery)) {
                pt.setInt(1, user_id);
                pt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
