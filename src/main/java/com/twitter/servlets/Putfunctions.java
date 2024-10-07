package com.twitter.servlets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class Putfunctions {
    /*public static void setUsername(Connection connection,String value, int user_id){
        String setUsernameQuery = "UPDATE user_details SET user_name = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setUsernameQuery)){
            pt.setString(1,value);
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setEmailId(Connection connection,String value, int user_id){
        String setEmailIdQuery = "UPDATE user_details SET email_id = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setEmailIdQuery)){
            pt.setString(1,value);
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setWebsiteLink(Connection connection,String value, int user_id){
        String setWebsiteLinkQuery = "UPDATE user_details SET website_link = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setWebsiteLinkQuery)){
            pt.setString(1,value);
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setIsProfessional(Connection connection, String value, int user_id){
        String setIsProfessionalQuery = "UPDATE user_details SET is_professional = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setIsProfessionalQuery)){
            pt.setBoolean(1, Boolean.parseBoolean(value));
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setPhoneNo(Connection connection, String value, int user_id){
        String setPhoneNoQuery = "UPDATE user_details SET phone_no = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setPhoneNoQuery)){
            pt.setString(1,value);
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setCountry(Connection connection, String value, int user_id){
        String setCountryQuery = "UPDATE user_details SET country = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setCountryQuery)){
            pt.setString(1,value);
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setProtectPost(Connection connection, String value, int user_id){
        String setProtectPostQuery = "UPDATE user_details SET protect_post = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setProtectPostQuery)){
            pt.setBoolean(1, Boolean.parseBoolean(value));
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setPhotoTagging(Connection connection, String value, int user_id){
        String setPhotoTaggingQuery = "UPDATE user_details SET photo_tagging = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setPhotoTaggingQuery)){
            pt.setBoolean(1, Boolean.parseBoolean(value));
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setDirectMsg(Connection connection, String value, int user_id){
        String setDirectMsgQuery = "UPDATE user_details SET direct_msg = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setDirectMsgQuery)){
            pt.setBoolean(1, Boolean.parseBoolean(value));
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setAllowCalls(Connection connection, String value, int user_id){
        String setAllowCallsQuery = "UPDATE user_details SET allow_calls = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setAllowCallsQuery)){
            pt.setBoolean(1, Boolean.parseBoolean(value));
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setReadReceipts(Connection connection, String value, int user_id){
        String setReadReceiptsQuery = "UPDATE user_details SET read_receipts = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setReadReceiptsQuery)){
            pt.setBoolean(1, Boolean.parseBoolean(value));
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setFindViaEmail(Connection connection, String value, int user_id){
        String setFindViaEmailQuery = "UPDATE user_details SET find_via_email = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setFindViaEmailQuery)){
            pt.setBoolean(1, Boolean.parseBoolean(value));
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setFindViaPhone(Connection connection, String value, int user_id){
        String setFindViaPhoneQuery = "UPDATE user_details SET find_via_phone = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setFindViaPhoneQuery)){
            pt.setBoolean(1, Boolean.parseBoolean(value));
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setPersonalizedAds(Connection connection, String value, int user_id){
        String setPersonalizedAdsQuery = "UPDATE user_details SET personalized_ads = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setPersonalizedAdsQuery)){
            pt.setBoolean(1, Boolean.parseBoolean(value));
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setPersonalizeById(Connection connection, String value, int user_id){
        String setPersonalizeByIdQuery = "UPDATE user_details SET personalize_by_id = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setPersonalizeByIdQuery)){
            pt.setBoolean(1, Boolean.parseBoolean(value));
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setInfShareWithBusiPartners(Connection connection, String value, int user_id){
        String setInfShareWithBusiPartnersQuery = "UPDATE user_details SET inf_share_with_busi_partners = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setInfShareWithBusiPartnersQuery)){
            pt.setBoolean(1, Boolean.parseBoolean(value));
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setPersonalizeByLocation(Connection connection, String value, int user_id){
        String setPersonalizeByLocationQuery = "UPDATE user_details SET set_personalize_by_location = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setPersonalizeByLocationQuery)){
            pt.setBoolean(1, Boolean.parseBoolean(value));
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public static void setAllowPostsWithGrok(Connection connection, String value, int user_id){
        String setAllowPostsWithGrokQuery = "UPDATE user_details SET set_allow_posts_with_grok = ? WHERE user_id = ?";
        try(PreparedStatement pt= connection.prepareStatement(setAllowPostsWithGrokQuery)){
            pt.setBoolean(1, Boolean.parseBoolean(value));
            pt.setInt(2,user_id);
            pt.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }*/


}
