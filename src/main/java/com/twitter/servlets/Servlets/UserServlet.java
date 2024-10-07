package com.twitter.servlets.Servlets;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.twitter.servlets.DBconnection.DBUtil;
import com.twitter.servlets.functions;
import com.twitter.servlets.models.userModel;
import com.twitter.servlets.models.connectedAccount;

public class UserServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        int user_id = functions.retrieveUserid(request);
        //converting json to variables
        if (user_id == 0) {
            userModel usermodel = mapper.readValue(request.getReader(), userModel.class);
            String username = usermodel.getName();
            String location = usermodel.getPlace();
            String websitelink = usermodel.getLink();
            boolean isprofessional = usermodel.isProfessionalAccOrNot();
            String phoneno = usermodel.getPhoneNo();
            String country = usermodel.getCountryLiving();
            String emailid = usermodel.getEmail();
            boolean isverified = usermodel.isVerifiedOrNot();
            String password = usermodel.getPass();
            boolean tfa = usermodel.isTwoFactorAuth();
            List<connectedAccount> connectedAccNode = usermodel.getAccountsConnected();

            System.out.println("test1");
            try (Connection connection = DBUtil.getConnection()) {
                connection.setAutoCommit(false);
                // 1)INSERT USER_TABLE QUERY
                int new_user_id = insertUserQuery(connection, username, location, websitelink,
                        isprofessional, phoneno, country, emailid, isverified);
                insertUserAdditionalDetails(connection, new_user_id);
                // 2) INSERT USER_AUTH TABLE
                if (new_user_id > 0) {
                    int newUAid = insertUAQuery(connection, new_user_id, password, tfa);
                    if (newUAid > 0) {
                        // 3) SETTING USER_PRIVACY TABLE TO DEFAULT FALSE
                        insertUserPrivacyDefault(connection, new_user_id);
                        // 4) SETTING USER_PERSONALIZATION TABLE TO DEFAULT FALSE
                        insertUserPersonalizationDefault(connection, new_user_id);
                        // 5) INSERT USER_CONNECT TABLE QUERY
                        insertUCQuery(connection, new_user_id, newUAid, connectedAccNode);
                        connection.commit();
                        connection.close();
                        response.setStatus(201);
                        response.setContentType("application/json");
                        PrintWriter out = response.getWriter();
                        out.print("{\"message\":\"Successfully added.\"}");
                    } else {
                        connection.rollback();
                        connection.close();
                        response.setStatus(401);
                        response.setContentType("application/json");
                        PrintWriter out = response.getWriter();
                        out.print("{\"message\": \"Failed to insert user_Auth table.\"}");
                    }
                } else {
                    connection.rollback();
                    connection.close();
                    response.setStatus(401);
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\": \"Failed to insert user_details table.\"}");
                }
            } catch (SQLException e) {
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Connection Error.\"}");
            }
        } else if (user_id > 0 && request.getMethod().equalsIgnoreCase("put")) {
            System.out.println(user_id);

            userModel updateUser = mapper.readValue(request.getReader(), userModel.class);

            System.out.println("Name: " + updateUser.getPlace());

            try (Connection connection = DBUtil.getConnection()) {
                connection.setAutoCommit(false);
                userModel existingUser = getUserFromDatabase(connection, user_id);
                if (existingUser == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("User not found");
                    return;
                }
                System.out.println("test5");
                if (updateUser.getName() != null && !updateUser.getName().isEmpty()) {
                    existingUser.setName(updateUser.getName());
                    System.out.println(existingUser.getName());
                }

                if (updateUser.getPlace() != null && !updateUser.getPlace().isEmpty()) {
                    existingUser.setPlace(updateUser.getPlace());
                }

                if (updateUser.getLink() != null && !updateUser.getLink().isEmpty()) {
                    existingUser.setLink(updateUser.getLink());
                }

                if (updateUser.isProfessionalAccOrNot() != existingUser.isProfessionalAccOrNot()) {
                    existingUser.setProfessionalAccOrNot(updateUser.isProfessionalAccOrNot());
                }

                if (updateUser.getPhoneNo() != null && !updateUser.getPhoneNo().isEmpty()) {
                    existingUser.setPhoneNo(updateUser.getPhoneNo());
                    System.out.println(existingUser.getPhoneNo());
                }

                if (updateUser.getCountryLiving() != null && !updateUser.getCountryLiving().isEmpty()) {
                    existingUser.setCountryLiving(updateUser.getCountryLiving());
                }

                if (updateUser.getEmail() != null && !updateUser.getEmail().isEmpty()) {
                    existingUser.setEmail(updateUser.getEmail());
                    System.out.println(existingUser.getEmail());
                }

                if (updateUser.isVerifiedOrNot() != existingUser.isVerifiedOrNot()) {
                    existingUser.setVerifiedOrNot(updateUser.isVerifiedOrNot());
                }

                if (updateUser.getPass() != null && !updateUser.getPass().isEmpty()) {
                    existingUser.setPass(updateUser.getPass());
                }

                if (updateUser.isTwoFactorAuth() != existingUser.isTwoFactorAuth()) {
                    existingUser.setTwoFactorAuth(updateUser.isTwoFactorAuth());
                }

                if (updateUser.isProtectPost() != existingUser.isProtectPost()) {
                    existingUser.setProtectPost(updateUser.isProtectPost());
                }
                if (updateUser.isProtectVideo() != existingUser.isProtectVideo()) {
                    existingUser.setProtectVideo(updateUser.isProtectVideo());
                }
                if (updateUser.isPhotoTagging() != existingUser.isPhotoTagging()) {
                    existingUser.setPhotoTagging(updateUser.isPhotoTagging());
                }
                if (updateUser.isDirectMsg() != existingUser.isDirectMsg()) {
                    existingUser.setDirectMsg(updateUser.isDirectMsg());
                }
                if (updateUser.isAllowCalls() != existingUser.isAllowCalls()) {
                    existingUser.setAllowCalls(updateUser.isAllowCalls());
                }
                if (updateUser.isReadReceipts() != existingUser.isReadReceipts()) {
                    existingUser.setReadReceipts(updateUser.isReadReceipts());
                }
                if (updateUser.isFindViaEmail() != existingUser.isFindViaEmail()) {
                    existingUser.setFindViaEmail(updateUser.isFindViaEmail());
                }
                if (updateUser.isFindViaPhone() != existingUser.isFindViaPhone()) {
                    existingUser.setFindViaPhone(updateUser.isFindViaPhone());
                }

                if (updateUser.isPersonalizedAds() != existingUser.isPersonalizedAds()) {
                    existingUser.setPersonalizedAds(updateUser.isPersonalizedAds());
                }
                if (updateUser.isPersonalizeById() != existingUser.isPersonalizeById()) {
                    existingUser.setPersonalizeById(updateUser.isPersonalizeById());
                }
                if (updateUser.isPersonalizeByLocation() != existingUser.isPersonalizeByLocation()) {
                    existingUser.setPersonalizeByLocation(updateUser.isPersonalizeByLocation());
                }
                if (updateUser.isInfShareWithBusiPartners() != existingUser.isInfShareWithBusiPartners()) {
                    existingUser.setInfShareWithBusiPartners(updateUser.isInfShareWithBusiPartners());
                }
                if (updateUser.isAllowPostsWithGrok() != existingUser.isAllowPostsWithGrok()) {
                    existingUser.setAllowPostsWithGrok(updateUser.isAllowPostsWithGrok());
                }
                if (updateUserProfile(connection, user_id, existingUser)) {
                    connection.commit();
                    response.setStatus(200);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Successfully updated.\"}");
                } else {
                    connection.rollback();
                    response.setStatus(401);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\": \"Failed to update table.\"}");
                }

            } catch (SQLException e) {
                System.out.println("OUT");
                e.printStackTrace();
            }

        }
    }


    private void insertUserAdditionalDetails(Connection connection,int user_id) throws SQLException {
        String insertUserAddDetailsQuery = "INSERT INTO user_add_details (user_id) VALUES (?)";
        try (PreparedStatement pt = connection.prepareStatement(insertUserAddDetailsQuery)){
            pt.setInt(1,user_id);
            pt.executeUpdate();
        } catch (SQLException e){
            connection.rollback();
            e.printStackTrace();
            System.out.println("OUT1");
        }
    }
    private boolean updateUserProfile(Connection connection, int user_id, userModel existingUser)
            throws SQLException {
        connection.setAutoCommit(false);
        String updateUserDetailsQuery = "UPDATE user_details SET user_name=?,location=?," +
                "website_link=?,is_professional=?,phone_no=?," +
                "country=?,email_id=?,is_verified=? WHERE user_id=?";
        try (PreparedStatement pt1 = connection.prepareStatement(updateUserDetailsQuery)) {
            pt1.setString(1, existingUser.getName());
            pt1.setString(2, existingUser.getPlace());
            pt1.setString(3, existingUser.getLink());
            pt1.setBoolean(4, existingUser.isProfessionalAccOrNot());
            pt1.setString(5, existingUser.getPhoneNo());
            pt1.setString(6, existingUser.getCountryLiving());
            pt1.setString(7, existingUser.getEmail());
            pt1.setBoolean(8, existingUser.isVerifiedOrNot());
            pt1.setInt(9, user_id);
            pt1.executeUpdate();
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
            System.out.println("OUT1");
            return false;
        }
        String updateUserAuthQuery = "UPDATE user_auth SET password=?,tfa=? WHERE user_id =?";
        try (PreparedStatement pt2 = connection.prepareStatement(updateUserAuthQuery)) {
            pt2.setString(1, existingUser.getPass());
            pt2.setBoolean(2, existingUser.isTwoFactorAuth());
            pt2.setInt(3, user_id);
            pt2.executeUpdate();
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
            System.out.println("OUT2");
            return false;
        }
        String updateUserPrivacyQuery = "UPDATE user_privacy SET protect_post=?,protect_video=?," +
                "photo_tagging=?,direct_msg=?," +
                "allow_calls=?,read_receipts=?,find_via_email=?,find_via_phone=? WHERE user_id=?";
        try (PreparedStatement pt3 = connection.prepareStatement(updateUserPrivacyQuery)) {
            pt3.setBoolean(1, existingUser.isProtectPost());
            pt3.setBoolean(2, existingUser.isProtectVideo());
            pt3.setBoolean(3, existingUser.isPhotoTagging());
            pt3.setBoolean(4, existingUser.isDirectMsg());
            pt3.setBoolean(5, existingUser.isAllowCalls());
            pt3.setBoolean(6, existingUser.isReadReceipts());
            pt3.setBoolean(7, existingUser.isFindViaEmail());
            pt3.setBoolean(8, existingUser.isFindViaPhone());
            pt3.setInt(9, user_id);
            pt3.executeUpdate();
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
            System.out.println("OUT3");
            return false;
        }
        String updateUserPersonalizationQuery = "UPDATE user_personalization SET personalized_ads=?," +
                "personalize_by_id=?," +
                "inf_share_with_busi_partners=?,personalize_by_location=?,allow_posts_with_grok=? " +
                "WHERE user_id=?";
        try (PreparedStatement pt4 = connection.prepareStatement(updateUserPersonalizationQuery)) {
            pt4.setBoolean(1, existingUser.isPersonalizedAds());
            pt4.setBoolean(2, existingUser.isPersonalizeById());
            pt4.setBoolean(3, existingUser.isInfShareWithBusiPartners());
            pt4.setBoolean(4, existingUser.isPersonalizeByLocation());
            pt4.setBoolean(5, existingUser.isAllowPostsWithGrok());
            pt4.setInt(6, user_id);
            pt4.executeUpdate();
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
            System.out.println("OUT4");
            return false;
        }
        connection.commit();
        return true;
    }

    private userModel getUserFromDatabase(Connection connection, int user_id) {
        userModel user = null;
        System.out.println("test0");

        String getUserQuery = "SELECT ud.user_name,ud.location,ud.website_link," +
                "ud.is_professional,ud.phone_no, ud.country, ud.email_id, ud.is_verified, " +
                "ua.password,ua.tfa," +
                "up.protect_post,up.protect_video,up.photo_tagging,up.direct_msg," +
                "up.allow_calls,up.read_receipts,up.find_via_email,up.find_via_phone, " +
                "upn.personalized_ads,upn.personalize_by_id,upn.inf_share_with_busi_partners," +
                "upn.personalize_by_location, allow_posts_with_grok " +
                "FROM user_details ud " +
                "JOIN user_auth ua ON ua.user_id=ud.user_id " +
                "JOIN user_privacy up ON ud.user_id=up.user_id " +
                "JOIN user_personalization upn ON ud.user_id=upn.user_id " +
                "WHERE ud.user_id=?";
        try (PreparedStatement pt = connection.prepareStatement(getUserQuery)) {
            pt.setInt(1, user_id);
            System.out.println("test123");
            ResultSet rs = pt.executeQuery();
            System.out.println("test0");
            if (rs.next()) {
                System.out.println("test1");
                user = new userModel();
                user.setName(rs.getString("user_name"));
                user.setPlace(rs.getString("location"));
                user.setLink(rs.getString("website_link"));
                user.setProfessionalAccOrNot(rs.getBoolean("is_professional"));
                user.setPhoneNo(rs.getString("phone_no"));
                user.setCountryLiving(rs.getString("country"));
                user.setEmail(rs.getString("email_id"));
                user.setVerifiedOrNot(rs.getBoolean("is_verified"));
                user.setPass(rs.getString("password"));
                user.setTwoFactorAuth(rs.getBoolean("tfa"));

                // Set privacy settings
                user.setProtectPost(rs.getBoolean("protect_post"));
                user.setProtectVideo(rs.getBoolean("protect_video"));
                user.setPhotoTagging(rs.getBoolean("photo_tagging"));
                user.setDirectMsg(rs.getBoolean("direct_msg"));
                user.setAllowCalls(rs.getBoolean("allow_calls"));
                user.setReadReceipts(rs.getBoolean("read_receipts"));
                user.setFindViaEmail(rs.getBoolean("find_via_email"));
                user.setFindViaPhone(rs.getBoolean("find_via_phone"));

                // Set personalization settings
                user.setPersonalizedAds(rs.getBoolean("personalized_ads"));
                user.setPersonalizeById(rs.getBoolean("personalize_by_id"));
                user.setInfShareWithBusiPartners(rs.getBoolean("inf_share_with_busi_partners"));
                user.setPersonalizeByLocation(rs.getBoolean("personalize_by_location"));
                user.setAllowPostsWithGrok(rs.getBoolean("allow_posts_with_grok"));

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    private int insertUserQuery(Connection connection, String username, String location,
                                String websitelink, boolean isprofessional, String phoneno,
                                String country, String emailid, boolean isverified) {
        String userQuery = "INSERT INTO user_details (user_name,location,website_link,is_professional," +
                "phone_no,country,email_id,is_verified) VALUES (?,?,?,?,?,?,?,?) RETURNING user_id";
        try (PreparedStatement pt = connection.prepareStatement(userQuery)) {
            pt.setString(1, username);
            pt.setString(2, location);
            pt.setString(3, websitelink);
            pt.setBoolean(4, isprofessional);
            pt.setString(5, phoneno);
            pt.setString(6, country);
            pt.setString(7, emailid);
            pt.setBoolean(8, isverified);

            ResultSet rs = pt.executeQuery();
            if (rs.next()) {
                return rs.getInt("user_id");
            } else {
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private int insertUAQuery(Connection connection, int new_user_id, String password, boolean tfa) {
        String uaQuery = "INSERT INTO user_auth (user_id,password,tfa) VALUES (?,?,?) RETURNING ua_id";
        try (PreparedStatement pt = connection.prepareStatement(uaQuery)) {
            pt.setInt(1, new_user_id);
            pt.setString(2, password);
            pt.setBoolean(3, tfa);

            ResultSet rs = pt.executeQuery();

            if (rs.next()) {
                return rs.getInt("ua_id");
            } else {
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void insertUserPrivacyDefault(Connection connection, int new_user_id) {
        String userPrivacyDefaultQuery = "INSERT INTO user_privacy(user_id,protect_post,protect_video," +
                "photo_tagging,direct_msg,allow_calls,read_receipts,find_via_email,find_via_phone) " +
                "VALUES (?,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE)";
        try (PreparedStatement pt = connection.prepareStatement(userPrivacyDefaultQuery)) {
            pt.setInt(1, new_user_id);
            pt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertUserPersonalizationDefault(Connection connection, int new_user_id) {
        String userPersonalizationDefaultQuery = "INSERT INTO user_personalization(user_id,personalized_ads," +
                "personalize_by_id,inf_share_with_busi_partners,personalize_by_location,allow_posts_with_grok) " +
                "VALUES (?,FALSE,FALSE,FALSE,FALSE,FALSE)";
        try (PreparedStatement pt = connection.prepareStatement(userPersonalizationDefaultQuery)) {
            pt.setInt(1, new_user_id);
            pt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertUCQuery(Connection connection, int new_user_id, int newUAid,
                               List<connectedAccount> connectedAccNode) {
        String userConnectQuery = "INSERT INTO user_connect(user_id,ua_id,connected_acc_type,connected_acc) " +
                "VALUES  (?,?,?,?)";
        try (PreparedStatement pt = connection.prepareStatement(userConnectQuery)) {
            for (connectedAccount s : connectedAccNode) {
                String connected_acc_type = s.getType_to_connect();
                String connected_acc = s.getAccount();
                pt.setInt(1, new_user_id);
                pt.setInt(2, newUAid);
                pt.setString(3, connected_acc_type);
                pt.setString(4, connected_acc);
                pt.addBatch();
            }
            pt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        userModel userdata = new userModel();
        try (Connection connection = DBUtil.getConnection()) {
            //GET USER_ID TO JOIN TABLES
            int user_id = functions.retrieveUserid(request);
            if (user_id <= 0) {
                throw new ServletException("User ID must be positive.");
            }
            System.out.println(user_id);
            //WRITE QUERIES USING JOINS
            String getUserDetailsQuery = "SELECT ud.user_name,ud.location,ud.website_link," +
                    "ud.is_professional,ud.phone_no, ud.country, ud.email_id, ud.is_verified, "
                    + "up.protect_post,up.protect_video,up.photo_tagging,up.direct_msg," +
                    "up.allow_calls,up.read_receipts,up.find_via_email,up.find_via_phone, "
                    + "upn.personalized_ads,upn.personalize_by_id,upn.inf_share_with_busi_partners," +
                    "upn.personalize_by_location, allow_posts_with_grok, "
                    + "uc.connected_acc_type,uc.connected_acc "
                    + "FROM user_details ud "
                    + "JOIN user_privacy up ON ud.user_id=up.user_id "
                    + "JOIN user_personalization upn ON ud.user_id=upn.user_id "
                    + "JOIN user_connect uc ON ud.user_id=uc.user_id "
                    + "WHERE ud.user_id=?";
            try (PreparedStatement pt = connection.prepareStatement(getUserDetailsQuery)) {
                pt.setInt(1, user_id);
                ResultSet rs = pt.executeQuery();
                while (rs.next()) {
                    userdata.setName(rs.getObject("user_name") != null ?
                            rs.getString("user_name") : null);
                    userdata.setPlace(rs.getObject("location") != null ?
                            rs.getString("location") : null);
                    userdata.setLink(rs.getObject("website_link") != null ?
                            rs.getString("website_link") : null);
                    userdata.setProfessionalAccOrNot(rs.getBoolean("is_professional"));
                    userdata.setPhoneNo(rs.getObject("phone_no") != null ?
                            rs.getString("phone_no") : null);
                    userdata.setCountryLiving(rs.getObject("country") != null ?
                            rs.getString("country") : null);
                    userdata.setEmail(rs.getObject("email_id") != null ?
                            rs.getString("email_id") : null);
                    userdata.setVerifiedOrNot(rs.getBoolean("is_verified"));
                    userdata.setProtectPost(rs.getBoolean("protect_post"));
                    userdata.setProtectVideo(rs.getBoolean("protect_video"));
                    userdata.setPhotoTagging(rs.getBoolean("photo_tagging"));
                    userdata.setDirectMsg(rs.getBoolean("direct_msg"));
                    userdata.setAllowCalls(rs.getBoolean("allow_calls"));
                    userdata.setReadReceipts(rs.getBoolean("read_receipts"));
                    userdata.setFindViaEmail(rs.getBoolean("find_via_email"));
                    userdata.setFindViaPhone(rs.getBoolean("find_via_phone"));
                    userdata.setPersonalizedAds(rs.getBoolean("personalized_ads"));
                    userdata.setPersonalizeById(rs.getBoolean("personalize_by_id"));
                    userdata.setInfShareWithBusiPartners(rs.getBoolean("inf_share_with_busi_partners"));
                    userdata.setPersonalizeByLocation(rs.getBoolean("personalize_by_location"));
                    userdata.setAllowPostsWithGrok(rs.getBoolean("allow_posts_with_grok"));
                    String type_to_connect = rs.getObject("connected_acc_type") != null ?
                            rs.getString("connected_acc_type") : null;
                    String account = rs.getObject("connected_acc") != null ?
                            rs.getString("connected_acc") : null;
                    if (type_to_connect != null && account != null) {
                        userdata.addConnectedAccounts(type_to_connect, account);
                    }
                }
                ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
                String jsonResult = writer.writeValueAsString(userdata);
                response.getWriter().write(jsonResult);
            } catch (SQLException e) {
                e.printStackTrace();
                connection.close();
                response.setStatus(404);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Failed to fetch.\"}");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
    //Map<String, String> jsonToDbMap = jsontoDBMap.getJsonToDbMap();
       /* int user_id = Integer.parseInt(request.getPathInfo().substring(1));
        if (user_id <= 0) {
            response.setStatus(400);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"Failed to update table.\"}");
            return;
        }
        //INPUT WOULD BE {KEY:{KEY:VALUE},KEY:{KEY:VALUE}}
        //{String:Object}-->OBJECT->{String:(Any Data Type(so pass it as Object)}
        StringBuilder jsonInput = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                jsonInput.append(line);
            }
        }

        String jsonString = jsonInput.toString();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(jsonString, Map.class);
        System.out.println("test1");
        //TEST TO PRINT KEY AND VALUE OF MAP(HERE KEY IS TABLE NAME AND VALUE IS ITS COLUMN NAME AND VALUE AS OBJECT)
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            if (map.containsKey("userDetails")) {
                Map<String, Object> userDetails = (Map<String, Object>) map.get("userDetails");
                System.out.println(userDetails);
                updateTable(connection, "user_details", userDetails, user_id, response);
            }
            if (map.containsKey("userAuth")) {
                Map<String, Object> userDetails = (Map<String, Object>) map.get("userAuth");
                System.out.println(userDetails);
                updateTable(connection, "user_auth", userDetails, user_id, response);
            }
            if (map.containsKey("userPrivacy")) {
                Map<String, Object> userDetails = (Map<String, Object>) map.get("userPrivacy");
                System.out.println(userDetails);
                updateTable(connection, "user_privacy", userDetails, user_id, response);
            }
            if (map.containsKey("userPersonalization")) {
                Map<String, Object> userDetails = (Map<String, Object>) map.get("userPersonalization");
                System.out.println(userDetails);
                updateTable(connection, "user_personalization", userDetails, user_id, response);
            }
            connection.commit();
            response.setStatus(200);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"Successfully updated.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(400);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"Failed to update table.\"}");
        }
    }

    private void updateTable(Connection connection, String tableName, Map<String, Object> data, int userId, HttpServletResponse response) throws SQLException, IOException {
        System.out.println("null1");

        System.out.println(data);

        //GET DATA OF MAPPED KEY VALUE OF JSON WITH DB COLUMN
        Map<String, String> KeyToDB = jsontoDBMap.getJsonToDbMap();

        StringBuilder queryBuilder = new StringBuilder("UPDATE ");
        queryBuilder.append(tableName);
        queryBuilder.append(" SET ");
        for (String col_key : data.keySet()) {
            //APPEND COLUMN NAME TO column VARIABLE BY FETCHING FROM ITS CORRESPONDING JSON VALUE
            String column = KeyToDB.get(col_key);
            System.out.print("column:");
            System.out.println(column);
            queryBuilder.append(column);
            queryBuilder.append(" = ?, ");
        }
        //REMOVE FINAL SPACE AND COMMA
        queryBuilder.delete(queryBuilder.length() - 2, queryBuilder.length());
        queryBuilder.append(" WHERE user_id = ?");
        System.out.println(queryBuilder);

        try (PreparedStatement pt = connection.prepareStatement(queryBuilder.toString())) {
            int index = 1;
            System.out.println(index);
            for (Object value : data.values()) {
                pt.setObject(index++, value);
            }
            pt.setInt(index, userId);
            pt.executeUpdate();
            System.out.println("Test2");
        } catch (SQLException e) {
            connection.rollback();
            connection.close();
            e.printStackTrace();
            response.setStatus(400);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print("{\"message\": \"Failed to update table.\"}");
        }
    }*/

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
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
        //GET THE TABLE NAMES THAT ROWS ARE TO BE DELETED FROM TABLES FROM CHILD TO PARENT TABLE
        String[] table_names = {"user_personalization", "user_privacy", "user_connect", "user_auth", "user_details"};
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            int user_id = functions.retrieveUserid1(connection,cred);
            if (user_id == -1) {
                connection.rollback();
                response.setStatus(401);
                PrintWriter out = response.getWriter();
                out.print("{\"message\": \"Invalid username or password.\"}");
                return;
            }
            for (String s : table_names) {
                String deleteDataQuery = "DELETE FROM " + s + " WHERE user_id = ?";
                try (PreparedStatement pt = connection.prepareStatement(deleteDataQuery)) {
                    pt.setInt(1, user_id);
                    int rowsAffected = pt.executeUpdate();
                    if (rowsAffected == 0) {
                        connection.close();
                        response.setStatus(404);
                        response.setContentType("application/json");
                        PrintWriter out = response.getWriter();
                        out.print("{\"message\":\"Data not found.\"}");
                        return;
                    }
                } catch (SQLException e) {
                    connection.rollback();
                    connection.close();
                    response.setStatus(500);
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"message\":\"Error deleting data from table: " + s + "\"}");
                    return;
                }
            }
            connection.commit();
            connection.close();
            response.setStatus(200);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print("{\"message\":\"Successfully deleted.\"}");
        } catch (SQLException e) {
            response.setStatus(404);
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print("{\"message\":\"Data not found.\"}");
        }
    }
}