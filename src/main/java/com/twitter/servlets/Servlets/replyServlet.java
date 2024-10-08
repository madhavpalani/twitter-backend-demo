package com.twitter.servlets.Servlets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;
import com.twitter.servlets.models.mentionsModel;
import com.twitter.servlets.models.replyModel;
import com.twitter.servlets.models.tagModel;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashSet;
import java.util.List;


public class replyServlet extends HttpServlet {
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
        }
        replyModel data = mapper.readValue(request.getReader(), replyModel.class);
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
            if ((data.getDelegatedID() != null) &&
                    !functions.checkDelegatedID(connection, user_id, data.getDelegatedID())) {
                response.setStatus(400);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Delegated ID don't have access.\"}");
                return;
            }
            if (!functions.checkPost(connection, data.getPostId())) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid Post ID.\"}");
                return;
            }
            int reply_id = insertReply(connection, data, user_id);
            if (reply_id <= 0) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Failed to insert reply table.\"}");
                return;
            }
            if (!insertPostTags(connection, data.getReplyTags(), reply_id)) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Failed to insert post_tags table.\"}");
                return;
            }
            if (!insertReplyMentions(connection, data.getReplyMentions(), reply_id)) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Failed to insert post_mentions table.\"}");
                return;
            }
            connection.commit();
            response.setStatus(200);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"Successfully added a Reply.\"}");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private int insertReply(Connection connection, replyModel data, int user_id) {
        String insertReplyQuery = "INSERT INTO replies_table (post_id, reply_content, user_id, c_id, delegated_id)" +
                " VALUES (?,?,?,?,?) RETURNING reply_id";
        try (PreparedStatement pt = connection.prepareStatement(insertReplyQuery)) {
            pt.setInt(1, data.getPostId());
            pt.setString(2, data.getText());
            pt.setInt(3, user_id);
            if (data.getCommunityID() != null && checkCommunity(connection,data.getCommunityID(),user_id)) {
                pt.setInt(4, data.getCommunityID());
            } else {
                pt.setNull(4, Types.INTEGER);
            }
            if (data.getDelegatedID() != null) {
                pt.setInt(5, data.getDelegatedID());
            } else {
                pt.setNull(5, Types.INTEGER);
            }
            ResultSet rs = pt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private boolean checkCommunity(Connection connection, int communityId, int userId){
        String checkCommunityQuery = "SELECT 1 FROM community_user WHERE c_id = ? AND user_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(checkCommunityQuery)){
            pt.setInt(1,communityId);
            pt.setInt(2,userId);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }


    private boolean insertPostTags(Connection connection, HashSet<tagModel> tagsData, int reply_id) {
        String insertPostQuery = "INSERT INTO post_tag (reply_id,tag_id) VALUES (?,?)";
        try (PreparedStatement pt = connection.prepareStatement(insertPostQuery)) {
            for (tagModel tags : tagsData) {
                pt.setInt(1, reply_id);
                pt.setInt(2, tags.getTagId());
                pt.addBatch();
            }
            pt.executeBatch();
            for (tagModel tags : tagsData) {
                if (!updateTagsUsed(connection, tags.getTagId(), true)) {
                    return false;
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Boolean updateTagsUsed(Connection connection, int tag_id, boolean flag) throws SQLException {
        if (flag) {
            String updateTagUsedQuery = "UPDATE tags SET used_count=used_count+1 WHERE tag_id = ?";
            try (PreparedStatement pt = connection.prepareStatement(updateTagUsedQuery)) {
                pt.setInt(1, tag_id);
                pt.executeUpdate();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
                return false;
            }
        } else {
            String updateTagUsedQuery = "UPDATE tags SET used_count=used_count-1 WHERE tag_id = ?";
            try (PreparedStatement pt = connection.prepareStatement(updateTagUsedQuery)) {
                pt.setInt(1, tag_id);
                pt.executeUpdate();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
                return false;
            }
        }
    }

    private boolean insertReplyMentions(Connection connection, HashSet<mentionsModel> mentionsData, int reply_id) {
        String insertMentionsQuery = "INSERT INTO post_mentions (user_id,reply_id,mention_name) VALUES (?,?,?)";
        try (PreparedStatement pt = connection.prepareStatement(insertMentionsQuery)) {
            for (mentionsModel data : mentionsData) {
                int mention_id = getMentionsId(connection, data.getMentionName());
                if (mention_id <= 0) {
                    return false;
                }
                pt.setInt(1, mention_id);
                pt.setInt(2, reply_id);
                pt.setString(3, data.getMentionName().substring(1));
                pt.addBatch();
            }
            pt.executeBatch();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private int getMentionsId(Connection connection, String data) {
        String fetchNameQuery = "SELECT user_id FROM user_details WHERE user_name = ?";
        if (data.startsWith("@")) {
            data = data.substring(1);
            System.out.println(data);
            System.out.println("true");
        }
        try (PreparedStatement pt = connection.prepareStatement(fetchNameQuery)) {
            pt.setString(1, data);
            ResultSet rs = pt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
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
        }
        String getReplyQuery = "SELECT rt.reply_id, rt.reply_content, rt.post_id, rt.likes, " +
                "rt.timestamp, rt.c_id, rt.delegated_id, ptg.tag_id,pm.user_id, pm.mention_name " +
                "FROM replies_table rt " +
                "LEFT JOIN post_tag ptg ON rt.reply_id = ptg.reply_id " +
                "LEFT JOIN post_mentions pm ON rt.reply_id = pm.reply_id " +
                "WHERE rt.user_id = ?";
        try (Connection connection = DBUtil.getConnection()) {
            int user_id = functions.retrieveUserid1(connection, cred);
            if (user_id == -1) {
                connection.rollback();
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            PreparedStatement pt = connection.prepareStatement(getReplyQuery);
            pt.setInt(1, user_id);
            ResultSet rs = pt.executeQuery();
            StringBuilder jsonResult = new StringBuilder();
            int last_reply_id = -1;
            boolean flag = true;
            jsonResult.append("[");
            while (rs.next()) {
                replyModel newReplyModel = new replyModel();
                int reply_id = rs.getInt("reply_id");
                if (reply_id != last_reply_id) {
                    flag = false;
                    newReplyModel.setText(rs.getString("reply_content"));
                    newReplyModel.setPostId(rs.getObject("post_id") != null ?
                            rs.getInt("post_id") : null);
                    newReplyModel.setCommunityID(rs.getObject("c_id") != null ?
                            rs.getInt("c_id") : null);
                    newReplyModel.setLikes(rs.getObject("likes") != null ?
                            rs.getInt("likes") : null);
                    newReplyModel.setTimestamp(rs.getObject("timestamp") != null ?
                            rs.getTimestamp("timestamp") : null);
                    newReplyModel.setDelegatedID(rs.getObject("delegated_id") != null ?
                            rs.getInt("delegated_id") : null);
                    last_reply_id = reply_id;
                }
                int tagId = rs.getObject("tag_id") != null ? rs.getInt("tag_id") : -1;
                int MentionID = rs.getObject("user_id") != null ? rs.getInt("user_id") : -1;
                String mentionName = rs.getString("mention_name");
                if (tagId != -1) {
                    newReplyModel.addTagID(tagId);
                    System.out.println(tagId);
                }
                if (MentionID != -1) {
                    newReplyModel.addMentionsName(MentionID, mentionName);
                }

                ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
                if (!flag) {
                    jsonResult.append(writer.writeValueAsString(newReplyModel));
                    flag = true;
                    jsonResult.append(",");
                }

            }
            if (jsonResult.length() > 1) {
                jsonResult.setLength(jsonResult.length() - 1);
            }
            jsonResult.append("]");
            response.getWriter().write(jsonResult.toString());
        } catch (SQLException e) {
            response.setStatus(404);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"Connection Error.\"}");
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
        }
        ObjectMapper mapper = new ObjectMapper();
        try (Connection connection = DBUtil.getConnection()) {
            int user_id = functions.retrieveUserid1(connection, cred);
            JsonNode jsonNode = mapper.readTree(request.getReader());
            int r_id = jsonNode.get("Reply_ID").asInt();
            connection.setAutoCommit(false);
            if (user_id == -1) {
                connection.rollback();
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            if (!checkReplyInTable(connection, r_id)) {
                connection.rollback();
                response.setStatus(404);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Reply not found.\"}");
                return;
            }
            if (getUserIDfromReply(connection, r_id) != user_id) {
                connection.rollback();
                response.setStatus(400);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"You dont have access.\"}");
                return;
            }
            updateTagsCountFromPost(connection, r_id);
            deleteReply(connection, r_id);
            connection.commit();
            response.setStatus(200);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"Successfully deleted.\"}");


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean checkReplyInTable(Connection connection, int r_id) throws SQLException {
        String checkPostQuery = "SELECT 1 FROM replies_table where reply_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(checkPostQuery)) {
            pt.setInt(1, r_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private int getUserIDfromReply(Connection connection, int r_id) {
        String getUserIDQuery = "SELECT user_id FROM replies_table WHERE reply_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(getUserIDQuery)) {
            pt.setInt(1, r_id);
            ResultSet rs = pt.executeQuery();
            if (rs.next()) {
                return rs.getInt("user_id");
            }
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void updateTagsCountFromPost(Connection connection, int r_id) throws SQLException {
        String updateTagCountFromPost = "SELECT tag_id FROM post_tag WHERE reply_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(updateTagCountFromPost)) {
            pt.setInt(1, r_id);
            ResultSet rs = pt.executeQuery();
            while (rs.next()) {
                updateTagsUsed(connection, rs.getInt("tag_id"), false);
            }
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
        }
    }

    private void deleteReply(Connection connection, int r_id) throws SQLException {
        String DeletePostQuery = "DELETE FROM replies_table WHERE reply_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(DeletePostQuery)) {
            pt.setInt(1, r_id);
            pt.executeUpdate();
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
        }
    }

}
