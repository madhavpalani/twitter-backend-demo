package com.twitter.servlets;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class functions {

    public static List<String> getAuthCredentials(String authHeader) {
        List<String> auth = new ArrayList<>();
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            byte[] decoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decoded, StandardCharsets.UTF_8);
            final String[] values = credentials.split(":", 2);
            auth.add(values[0]);
            auth.add(values[1]);
            return auth;
        } else {
            return auth;
        }
    }

    public static int retrieveUserid1(Connection connection, List<String> cred) {
        String retrieveIDQuery = "SELECT ua.user_id FROM user_auth ua JOIN user_details ud ON ua.user_id=ud.user_id WHERE ud.user_name = ? AND " +
                "ua.password = ? ";
        try (PreparedStatement pt = connection.prepareStatement(retrieveIDQuery)) {
            pt.setString(1, cred.get(0));
            pt.setString(2, cred.get(1));
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

    public static int retrieveUserid(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            return 0;
        } else if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
            return Integer.parseInt(pathInfo);
        }
        return 0;
    }

    public static boolean updateCommunityUser(Connection connection, int user_id, int c_id, String role) {
        String updateCommunityUser = "INSERT INTO community_user (user_id,c_id,role_name) VALUES (?,?,?) RETURNING cu_id";
        try (PreparedStatement pt = connection.prepareStatement(updateCommunityUser)) {
            pt.setInt(1, user_id);
            pt.setInt(2, c_id);
            pt.setString(3, role);
            ResultSet rs = pt.executeQuery();
            while (rs.next()) {
                return true;
            }
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateCountInCommunityTable(Connection connection, int c_id, boolean flag) {
        if (flag) {
            String AddCountQuery = "UPDATE community_table SET user_count=user_count+1 WHERE c_id=?";
            try (PreparedStatement pt = connection.prepareStatement(AddCountQuery)) {
                pt.setInt(1, c_id);
                pt.executeUpdate();
                return true;

            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            String SubCountQuery = "UPDATE community_table SET user_count=user_count-1 WHERE c_id=?";
            try (PreparedStatement pt = connection.prepareStatement(SubCountQuery)) {
                pt.setInt(1, c_id);
                pt.executeUpdate();
                return true;

            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static boolean removeCommunityUserData(Connection connection, int c_id) {
        String removeCommunityUserDataQuery = "DELETE FROM community_user WHERE c_id=?";
        try (PreparedStatement pt = connection.prepareStatement(removeCommunityUserDataQuery)) {
            pt.setInt(1, c_id);
            pt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int getCommunityID(Connection connection, String name) {
        String getCommunityIDQuery = "SELECT c_id FROM community_table WHERE c_name=?";
        try (PreparedStatement pt = connection.prepareStatement(getCommunityIDQuery)) {
            pt.setString(1, name);
            ResultSet rs = pt.executeQuery();
            while (rs.next()) {
                return rs.getInt("c_id");
            }
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void updateUserCommIn(Connection connection, int c_id, boolean flag) {
        if (flag) {
            String getCountQuery = "UPDATE user_add_details SET communities_in=communities_in-1 WHERE user_id IN " +
                    "(SELECT DISTINCT user_id FROM community_user WHERE c_id=?)";
            try (PreparedStatement pt = connection.prepareStatement(getCountQuery)) {
                pt.setInt(1, c_id);
                pt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            String getCountQuery = "UPDATE user_add_details SET communities_in=communities_in+1 WHERE user_id IN " +
                    "(SELECT DISTINCT user_id FROM community_user WHERE c_id=?)";
            try (PreparedStatement pt = connection.prepareStatement(getCountQuery)) {
                pt.setInt(1, c_id);
                pt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    //replies
    public static boolean checkDelegatedID(Connection connection, int user_id, int del_id) {
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

    public static boolean checkPost(Connection connection, int post_id) {
        String checkPostQuery = "SELECT 1 FROM post_table WHERE post_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(checkPostQuery)) {
            pt.setInt(1, post_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean checkAlreadyLiked(Connection connection, int post_id, int user_id, boolean flag) throws SQLException {
        if (flag) {
            String checkAlreadyLikedQuery = "SELECT 1 FROM post_like WHERE post_id = ? AND user_id = ?";
            try (PreparedStatement pt = connection.prepareStatement(checkAlreadyLikedQuery)) {
                pt.setInt(1, post_id);
                pt.setInt(2, user_id);
                ResultSet rs = pt.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                e.printStackTrace();
                connection.rollback();
                return false;
            }
        } else {
            String checkAlreadyLikedQuery = "SELECT 1 FROM post_like WHERE reply_id = ? AND user_id = ?";
            try (PreparedStatement pt = connection.prepareStatement(checkAlreadyLikedQuery)) {
                pt.setInt(1, post_id);
                pt.setInt(2, user_id);
                ResultSet rs = pt.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                e.printStackTrace();
                connection.rollback();
                return false;
            }
        }
    }

}

