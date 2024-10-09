package com.twitter.servlets.Servlets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;
import com.twitter.servlets.models.mentionsModel;
import com.twitter.servlets.models.postDetailsModel;
import com.twitter.servlets.models.tagModel;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashSet;
import java.util.List;

public class postServlet extends HttpServlet {
    private JedisPool jedisPool;

    @Override
    public void init() throws ServletException {
        super.init();
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(50);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(2);

        jedisPool = new JedisPool(poolConfig, "localhost", 6379);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        ObjectMapper mapper = new ObjectMapper();
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if (cred.isEmpty()) {
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
        }
        postDetailsModel postData = mapper.readValue(request.getReader(), postDetailsModel.class);
        try (Connection connection = DBUtil.getConnection()) {
            int user_id = functions.retrieveUserid1(connection, cred);
            connection.setAutoCommit(false);
            if (user_id == -1) {
                connection.rollback();
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            if (postData.getDelegatedId() != null && !checkDelegatedID(connection, user_id,
                    postData.getDelegatedId())) {
                response.setStatus(400);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Delegated ID don't have access.\"}");
                return;
            }
            //1)INSERT INTO POST_TABLE
            int post_id = insertPost(connection, postData, user_id);
            if (post_id <= 0) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Failed to insert post_details table.\"}");
                return;
            }
            //2)INSERT POST_TAGS
            if (!insertPostTags(connection, postData.getPostTags(), post_id)) {
                connection.rollback();
                connection.close();
                response.setStatus(401);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Failed to insert post_tags table.\"}");
                return;
            }
            //3)INSERT POST_MENTIONS
            if (!insertPostMentions(connection, postData.getPostMentions(), post_id)) {
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
            out.print("{\"message\": \"Successfully added a post.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int insertPost(Connection connection, postDetailsModel postData, int user_id) {
        String insertPostDataQuery = "INSERT INTO post_table (content,user_id,parent_post_id,c_id,delegated_id) " +
                "VALUES (?,?,?,?,?) RETURNING post_id";
        try (PreparedStatement pt = connection.prepareStatement(insertPostDataQuery)) {
            pt.setString(1, postData.getText());
            pt.setInt(2, user_id);
            if (postData.getParentPostId() != null) {
                pt.setInt(3, postData.getParentPostId());
                updateRepostsCount(connection, postData.getParentPostId(), true);

            } else {
                pt.setNull(3, Types.INTEGER);
            }
            if (postData.getCommunityId() != null) {
                pt.setInt(4, postData.getCommunityId());
            } else {
                pt.setNull(4, Types.INTEGER);
            }
            if (postData.getDelegatedId() != null) {
                pt.setInt(5, postData.getDelegatedId());
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

    private boolean checkDelegatedID(Connection connection, int user_id, int del_id) {
        String checkDelegatesQuery = "SELECT 1 FROM delegated_table WHERE user_id = ? AND delegated_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(checkDelegatesQuery)) {
            pt.setInt(1, user_id);
            pt.setInt(2, del_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    private boolean insertPostTags(Connection connection, HashSet<tagModel> tagsData, int post_id) {
        String insertPostQuery = "INSERT INTO post_tag (post_id,tag_id) VALUES (?,?)";
        try (PreparedStatement pt = connection.prepareStatement(insertPostQuery)) {
            for (tagModel tags : tagsData) {
                pt.setInt(1, post_id);
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

    private boolean insertPostMentions(Connection connection, HashSet<mentionsModel> mentionsData, int post_id) {
        String insertMentionsQuery = "INSERT INTO post_mentions (user_id,post_id,mention_name) VALUES (?,?,?)";
        try (PreparedStatement pt = connection.prepareStatement(insertMentionsQuery)) {
            for (mentionsModel data : mentionsData) {
                int mention_id = getMentionsId(connection, data.getMentionName());
                if (mention_id <= 0) {
                    return false;
                }
                pt.setInt(1, mention_id);
                pt.setInt(2, post_id);
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

    private Boolean updateTagsUsed(Connection connection, int tag_id, boolean flag)
            throws SQLException {
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

    private void updateRepostsCount(Connection connection, Integer post_id, boolean flag)
            throws SQLException {
        if (flag) {
            String updateRepostsCountQuery = "UPDATE post_table SET reposts = reposts+1 WHERE post_id = ? " +
                    "RETURNING parent_post_id";
            try (PreparedStatement pt = connection.prepareStatement(updateRepostsCountQuery)) {
                pt.setInt(1, post_id);
                ResultSet rs = pt.executeQuery();
                if (rs.next()) {
                    int id = rs.getObject(1) != null ? rs.getInt(1) : -1;
                    if (id != -1) {
                        updateRepostsCount(connection, id, true);
                    }
                }

            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        } else {
            String updateRepostsCountQuery = "UPDATE post_table SET reposts = reposts-1 WHERE post_id = ? " +
                    "RETURNING parent_post_id";
            try (PreparedStatement pt = connection.prepareStatement(updateRepostsCountQuery)) {
                pt.setInt(1, post_id);
                ResultSet rs = pt.executeQuery();
                if (rs.next()) {
                    int id = rs.getObject(1) != null ? rs.getInt(1) : -1;
                    if (id != -1) {
                        updateRepostsCount(connection, id, false);
                    }
                }

            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws
            ServletException, IOException {
        try (Jedis jedis = jedisPool.getResource()) {
            response.setContentType("application/json");
            String authHeader = request.getHeader("Authorization");
            List<String> cred = functions.getAuthCredentials(authHeader);
            if (cred.isEmpty()) {
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"No credentials.\"}");
            }
            String getPostsQuery = "SELECT pt.post_id, pt.content, pt.parent_post_id, pt.likes, pt.reposts, " +
                    "pt.timestamp, pt.c_id, pt.delegated_id, ptg.tag_id,pm.user_id, pm.mention_name " +
                    "FROM post_table pt " +
                    "LEFT JOIN post_tag ptg ON pt.post_id = ptg.post_id " +
                    "LEFT JOIN post_mentions pm ON pt.post_id = pm.post_id " +
                    "WHERE pt.c_id IN (SELECT c_id FROM community_user WHERE user_id = ?) " +
                    "OR pt.post_id IN(SELECT post_id FROM post_mentions WHERE user_id = ?) ORDER BY timestamp DESC";
            try (Connection connection = DBUtil.getConnection()) {
                int user_id = functions.retrieveUserid1(connection, cred);
                if (user_id == -1) {
                    connection.rollback();
                    response.setStatus(401);
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\": \"Invalid username or password.\"}");
                    return;
                }
                if (jedis.exists(String.valueOf(user_id))) {
                    String jsonResult = jedis.get(String.valueOf(user_id));
                    response.setStatus(200);
                    response.getWriter().write(jsonResult);
                    System.out.println("Got from Cache");
                    return;
                }
                PreparedStatement pt = connection.prepareStatement(getPostsQuery);
                pt.setInt(1, user_id);
                pt.setInt(2, user_id);
                ResultSet rs = pt.executeQuery();
                StringBuilder jsonResult = new StringBuilder();
                int last_post_id = -1;
                boolean flag = true;
                jsonResult.append("[");
                while (rs.next()) {
                    postDetailsModel newPostDetailModel = new postDetailsModel();
                    int post_id = rs.getInt("post_id");
                    if (post_id != last_post_id) {
                        flag = false;
                        newPostDetailModel.setText(rs.getString("content"));
                        newPostDetailModel.setParentPostId(rs.getObject("parent_post_id") != null
                                ? rs.getInt("parent_post_id") : null);
                        newPostDetailModel.setCommunityId(rs.getObject("c_id") != null
                                ? rs.getInt("parent_post_id") : null);
                        newPostDetailModel.setLikes(rs.getObject("likes") != null
                                ? rs.getInt("likes") : null);
                        newPostDetailModel.setCommunityId(rs.getObject("c_id") != null
                                ? rs.getInt("c_id") : null);
                        newPostDetailModel.setReposts(rs.getObject("reposts") != null
                                ? rs.getInt("reposts") : null);
                        newPostDetailModel.setTimestamp(rs.getObject("timestamp") != null
                                ? rs.getTimestamp("timestamp") : null);
                        newPostDetailModel.setDelegatedId(rs.getObject("delegated_id") != null
                                ? rs.getInt("delegated_id") : null);
                        last_post_id = post_id;
                    }
                    int tagId = rs.getObject("tag_id") != null ? rs.getInt("tag_id") : -1;
                    String mentionName = rs.getString("mention_name");
                    if (tagId != -1) {
                        newPostDetailModel.addTagId(tagId);
                        System.out.println(tagId);
                    }
                    if (mentionName != null) {
                        newPostDetailModel.addMentionNames(mentionName);
                        System.out.println(mentionName);
                    }
                    ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
                    if (!flag) {
                        jsonResult.append(writer.writeValueAsString(newPostDetailModel));
                        flag = true;
                        jsonResult.append(",");
                    }
                }
                if (jsonResult.length() > 1) {
                    jsonResult.setLength(jsonResult.length() - 1);
                }
                jsonResult.append("]");
                jedis.setex(String.valueOf(user_id),30,jsonResult.toString());
                response.getWriter().write(jsonResult.toString());
            } catch (SQLException e) {
                response.setStatus(404);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Connection Error.\"}");
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws
            ServletException, IOException {
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
            int p_id = jsonNode.get("PostID").asInt();
            connection.setAutoCommit(false);
            if (user_id == -1) {
                connection.rollback();
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            if (!checkPostInTable(connection, p_id)) {
                connection.rollback();
                response.setStatus(404);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Post not found.\"}");
                return;
            }
            if (getUserIDfromPost(connection, p_id) != user_id) {
                connection.rollback();
                response.setStatus(400);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"You dont have access.\"}");
                return;
            }
            updateTagsCountFromPost(connection, p_id);
            updateRepostsCount(connection, p_id, false);
            deletePost(connection, p_id);
            connection.commit();
            response.setStatus(200);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"Successfully deleted.\"}");


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getUserIDfromPost(Connection connection, int p_id) {
        String getUserIDQuery = "SELECT user_id FROM post_table WHERE post_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(getUserIDQuery)) {
            pt.setInt(1, p_id);
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

    private boolean checkPostInTable(Connection connection, int p_id) throws SQLException {
        String checkPostQuery = "SELECT 1 FROM post_table where post_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(checkPostQuery)) {
            pt.setInt(1, p_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void updateTagsCountFromPost(Connection connection, int p_id) throws SQLException {
        String updateTagCountFromPost = "SELECT tag_id FROM post_tag WHERE post_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(updateTagCountFromPost)) {
            pt.setInt(1, p_id);
            ResultSet rs = pt.executeQuery();
            while (rs.next()) {
                updateTagsUsed(connection, rs.getInt("tag_id"), false);
            }
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
        }
    }

    private void deletePost(Connection connection, int p_id) throws SQLException {
        String DeletePostQuery = "DELETE FROM post_table WHERE post_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(DeletePostQuery)) {
            pt.setInt(1, p_id);
            pt.executeUpdate();
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
        }
    }
    public void destroy() {
        if (jedisPool != null) {
            jedisPool.close();
        }
        super.destroy();
    }

}