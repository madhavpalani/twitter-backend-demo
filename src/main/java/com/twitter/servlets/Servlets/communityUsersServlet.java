package com.twitter.servlets.Servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;
import com.twitter.servlets.models.User;
import com.twitter.servlets.models.communityUsersModel;

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

public class communityUsersServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        String authHeader = request.getHeader("Authorization");
        ObjectMapper mapper = new ObjectMapper();
        List<String> cred = functions.getAuthCredentials(authHeader);
        if(cred.isEmpty()){
            response.setStatus(400);
            PrintWriter out = response.getWriter();
            out.println("{\"Message\":\"Invalid Username and Password\"}");
            return;
        }
        communityUsersModel newData = mapper.readValue(request.getReader(),communityUsersModel.class);


        String getCommunityUsersQuery = "SELECT user_name FROM user_details WHERE user_id IN" +
                "(SELECT user_id FROM community_user WHERE c_id = ?)";
        try (Connection connection = DBUtil.getConnection()){
            StringBuilder jsonResult = new StringBuilder();
            connection.setAutoCommit(false);
            int user_id = functions.retrieveUserid1(connection,cred);
            if(user_id==-1){
                response.setStatus(400);
                PrintWriter out = response.getWriter();
                out.println("{\"Message\":\"Invalid Username and Password\"}");
                return;
            }
            int c_id=newData.getCommunityId();
            if(!checkIDinCommunity(connection, newData.getCommunityId())){
                response.setStatus(404);
                PrintWriter out = response.getWriter();
                out.println("{\"Message\":\"Community ID not found\"}");
                return;
            }
            PreparedStatement pt = connection.prepareStatement(getCommunityUsersQuery);
            pt.setInt(1,c_id);
            ResultSet rs = pt.executeQuery();
            while (rs.next()){
                newData.addUserName(rs.getString(1));
            }
            ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
            jsonResult.append(writer.writeValueAsString(newData));
            response.setStatus(200);
            response.getWriter().write(jsonResult.toString());

        } catch (SQLException e){
            e.printStackTrace();
            response.setStatus(400);
            PrintWriter out = response.getWriter();
            out.println("{\"Message\":\"Connection Error\"}");
        }
    }
    private boolean checkIDinCommunity(Connection connection, int c_id){
        String checkIDInCommunity = "SELECT 1 FROM community_table WHERE c_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(checkIDInCommunity)){
            pt.setInt(1,c_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }
}
