package com.twitter.servlets.Servlets;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import java.util.Objects;

import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;
import com.twitter.servlets.models.CommunityModel;

public class communityServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        ObjectMapper mapper = new ObjectMapper();
        if(cred.isEmpty()){
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
            return;
        }
        if(!request.getMethod().equalsIgnoreCase("put")) {
            try (Connection connection = DBUtil.getConnection()) {
                connection.setAutoCommit(false);
                int user_id = functions.retrieveUserid1(connection, cred);
                if (user_id == -1) {
                    response.setStatus(401);
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\": \"Invalid username or password.\"}");
                    return;
                }
                if (!(checkVerified(connection, user_id))) {
                    response.setStatus(400);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"User is not Verified to create community.\"}");
                    return;
                }
                CommunityModel NewCommunity = mapper.readValue(request.getReader(), CommunityModel.class);
                if (createCommunity(connection, NewCommunity, user_id)) {
                    connection.commit();
                    response.setStatus(200);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Successfully Created.\"}");
                } else {
                    response.setStatus(400);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Error.\"}");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        else if (request.getMethod().equalsIgnoreCase("put")) {
            try (Connection connection = DBUtil.getConnection()) {
                CommunityModel NewCommunity = mapper.readValue(request.getReader(), CommunityModel.class);
                connection.setAutoCommit(false);
                int user_id = functions.retrieveUserid1(connection, cred);
                if (user_id == -1) {
                    connection.rollback();
                    response.setStatus(401);
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\": \"Invalid username or password.\"}");
                    return;
                }
                if (!(checkCreator(connection, user_id,NewCommunity.getCommunityName()))) {
                    connection.rollback();
                    response.setStatus(400);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"You are not creator.\"}");
                    return;
                }
                if(updateCommunity(connection, NewCommunity)){
                    connection.commit();
                    response.setStatus(200);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Successfully Updated.\"}");
                }
                else{
                    connection.rollback();
                    response.setStatus(400);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Error.\"}");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean createCommunity(Connection connection, CommunityModel NewCommunity, int user_id)
            throws SQLException {
        String createCommunityQuery = "INSERT INTO community_table (c_name,description,category,creator_id) " +
                "ALUES (?,?,?,?) RETURNING c_id";
        try (PreparedStatement pt = connection.prepareStatement(createCommunityQuery)) {
            pt.setString(1, NewCommunity.getCommunityName());
            pt.setString(2, NewCommunity.getAbout());
            pt.setString(3, NewCommunity.getType());
            pt.setInt(4, user_id);
            ResultSet rs = pt.executeQuery();
            if (rs.next()) {
                if (functions.updateCommunityUser(connection, user_id, rs.getInt("c_id"), "creator")
                        && functions.updateCountInCommunityTable(connection, rs.getInt("c_id"), true)) {
                    functions.updateUserCommIn(connection, rs.getInt("c_id"), false);
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
            return false;
        }
    }
    private boolean updateCommunity(Connection connection, CommunityModel NewCommunity) throws SQLException {
        StringBuilder createCommunityQuery = new StringBuilder();
        createCommunityQuery.append("UPDATE community_table SET ");
        if(NewCommunity.getAbout()==null && NewCommunity.getType()==null){
            return false;
        }
        if(!Objects.equals(NewCommunity.getAbout(), "")) {
            createCommunityQuery.append("description = ?, ");
        }
        if(!Objects.equals(NewCommunity.getType(), "")){
            createCommunityQuery.append("category = ? ");
        }
        createCommunityQuery.append("WHERE c_id = ? RETURNING c_id");
        int c_id = functions.getCommunityID(connection, NewCommunity.getCommunityName());
        int x=1;
        try (PreparedStatement pt = connection.prepareStatement(String.valueOf(createCommunityQuery))) {
            if(NewCommunity.getAbout()==null && NewCommunity.getType()==null){
                return false;
            }
            if(!Objects.equals(NewCommunity.getAbout(), "")) {
                pt.setString(x++, NewCommunity.getAbout());
            }
            if(!Objects.equals(NewCommunity.getType(), "")){
                pt.setString(x++, NewCommunity.getType());
            }
            pt.setInt(x++, c_id);
            ResultSet rs =pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
            return false;
        }
    }

    private boolean checkCreator(Connection connection,int user_id, String communityName){
        String checkCreatorQuery = "SELECT 1 FROM community_table WHERE  creator_id = ? AND c_name = ?";
        try (PreparedStatement pt = connection.prepareStatement(checkCreatorQuery)){
            pt.setInt(1,user_id);
            pt.setString(2,communityName);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    private boolean checkVerified(Connection connection, int user_id) {
        String CheckVerifiedQuery = "SELECT 1 FROM user_details WHERE user_id = ? AND is_verified = true";
        try (PreparedStatement pt = connection.prepareStatement(CheckVerifiedQuery)) {
            pt.setInt(1, user_id);
            ResultSet rs = pt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if(cred.isEmpty()){
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
            return;
        }
        response.setContentType("/application/json");
        String temp1;
        try (Connection connection = DBUtil.getConnection()) {
            int user_id = functions.retrieveUserid1(connection,cred);
            if (user_id == -1) {
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            String getCommunityQuery = "SELECT c_name,description,user_count,category,creator_id FROM community_table";
            PreparedStatement pt = connection.prepareStatement(getCommunityQuery);
            ResultSet rs = pt.executeQuery();
            StringBuilder jsonResult = new StringBuilder();
            jsonResult.append("[");
            while (rs.next()) {
                temp1 = getCreatorName(connection, rs.getInt("creator_id"));
                jsonResult.append("{");
                jsonResult.append("\"CommunityName\":\"").append(rs.getString("c_name")).append("\",\n");
                jsonResult.append("\"About\":\"").append(rs.getString("description")).append("\",\n");
                jsonResult.append("\"Count\":\"").append(rs.getString("user_count")).append("\",\n");
                jsonResult.append("\"Type\":\"").append(rs.getString("category")).append("\",\n");
                jsonResult.append("\"CreatedBy\":\"").append(temp1).append("\",\n");
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
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"Some error.\"}");
        }
    }

    public static String getCreatorName(Connection connection, int user_id) {
        String getCreatorName = "SELECT user_name FROM user_details WHERE user_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(getCreatorName)) {
            pt.setInt(1, user_id);
            ResultSet rs = pt.executeQuery();
            if (rs.next()) {
                return rs.getString("user_name");
            }
            return "";
        } catch (SQLException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request,response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        String name;
        String authHeader = request.getHeader("Authorization");
        List<String> cred = functions.getAuthCredentials(authHeader);
        if(cred.isEmpty()){
            response.setStatus(401);
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"No credentials.\"}");
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        CommunityModel newModel =  mapper.readValue(request.getReader(),CommunityModel.class);
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            int user_id = functions.retrieveUserid1(connection,cred);
            if (user_id == -1) {
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            int c_id = functions.getCommunityID(connection, newModel.getCommunityName());
            if(!checkCommunityOwner(connection,user_id,c_id)){
                response.setStatus(400);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"message\":\"You are not the creator of this community\"}");
                return;
            }
            if (c_id != -1) {
                functions.updateUserCommIn(connection, c_id, true);
                if (functions.removeCommunityUserData(connection, c_id)) {
                    String deleteCommunityQuery = "DELETE FROM community_table WHERE c_id = ?";
                    PreparedStatement pt = connection.prepareStatement(deleteCommunityQuery);
                    pt.setInt(1,c_id);
                    pt.executeUpdate();
                    connection.commit();
                    response.setStatus(200);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Successfully deleted.\"}");
                } else {
                    connection.rollback();
                    response.setStatus(404);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Error.\"}");
                }
                return;
            }
            connection.rollback();
            response.setStatus(404);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print("{\"message\":\"ID NOT FOUND.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean checkCommunityOwner(Connection connection, int user_id, int c_id){
        String checkCommunityOwnerQuery = "SELECT 1 FROM community_table WHERE creator_id = ? AND c_id = ?";
        try (PreparedStatement pt = connection.prepareStatement(checkCommunityOwnerQuery)){
            pt.setInt(1,user_id);
            pt.setInt(2,c_id);
            ResultSet rs= pt.executeQuery();
            return rs.next();
        } catch(SQLException e){
            e.printStackTrace();
            return false;
        }
    }
}
