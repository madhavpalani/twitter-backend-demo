package com.twitter.servlets.Servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;
import com.twitter.servlets.models.userAddDetailsModel;

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

public class userAddDetailsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        String authHeader = request.getHeader("Authorization");
        ObjectMapper mapper = new ObjectMapper();
        List<String> cred = functions.getAuthCredentials(authHeader);
        StringBuilder jsonResult = new StringBuilder();
        if (cred.isEmpty()) {
            response.setStatus(400);
            PrintWriter out = response.getWriter();
            out.println("{\"Message\":\"Invalid Username and Password\"}");
            return;
        }
        String userAddDetailsQuery = "SELECT uad.followers_count, uad.following_count, " +
                "uad.communities_in, uad.tags_followed,count(pt.post_id) as post_count" +
                " FROM user_add_details uad JOIN post_table pt ON uad.user_id = pt.user_id " +
                "WHERE uad.user_id = ? " +
                "GROUP BY uad.followers_count, uad.following_count, uad.communities_in, uad.tags_followed";
        try (Connection connection = DBUtil.getConnection()){
            connection.setAutoCommit(false);
            int user_id = functions.retrieveUserid1(connection,cred);
            if (user_id == -1) {
                connection.rollback();
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            PreparedStatement pt = connection.prepareStatement(userAddDetailsQuery);
            pt.setInt(1,user_id);
            ResultSet rs = pt.executeQuery();
            userAddDetailsModel newData = new userAddDetailsModel();
            while (rs.next()){

                newData.setFollowersCount(rs.getInt("followers_count"));
                newData.setFollowingCount(rs.getInt("following_count"));
                newData.setCommunitiesIn(rs.getInt("communities_in"));
                newData.setTagsFollowed(rs.getInt("tags_followed"));
                newData.setPostCount(rs.getInt("post_count"));
            }
            ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
            jsonResult.append(writer.writeValueAsString(newData));
            response.getWriter().write(jsonResult.toString());
            response.setStatus(200);
        } catch (SQLException e){
            e.printStackTrace();
            response.setStatus(404);
            PrintWriter out = response.getWriter();
            out.println("{\"message\":\"Query error\"");
        }
    }
}
