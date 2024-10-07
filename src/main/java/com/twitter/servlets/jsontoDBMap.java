package com.twitter.servlets;

import java.util.HashMap;
import java.util.Map;

public class jsontoDBMap {

    public static Map<String, String> getJsonToDbMap() {
        Map<String, String> jsonToDbMap = new HashMap<>();

        jsonToDbMap.put("name", "user_name");
        jsonToDbMap.put("place", "location");
        jsonToDbMap.put("link", "website_link");
        jsonToDbMap.put("professional_acc_or_not", "is_professional");
        jsonToDbMap.put("phone_number", "phone_no");
        jsonToDbMap.put("country_living", "country");
        jsonToDbMap.put("mail", "email_id");
        jsonToDbMap.put("verified_or_not", "is_verified");
        jsonToDbMap.put("post_to_be_protected", "protect_post");
        jsonToDbMap.put("post_can_be_tagged", "photo_tagging");
        jsonToDbMap.put("dm_permissions", "direct_msg");
        jsonToDbMap.put("allow_to_call", "allow_calls");
        jsonToDbMap.put("read_receipt_or_not", "read_receipts");
        jsonToDbMap.put("acc_shown_via_email", "find_via_email");
        jsonToDbMap.put("acc_shown_via_phone", "find_via_phone");
        jsonToDbMap.put("ad_personalization", "personalized_ads");
        jsonToDbMap.put("identity_personalization", "personalize_by_id");
        jsonToDbMap.put("information_shared_with_business_partners", "inf_share_with_busi_partners");
        jsonToDbMap.put("ads_by_location", "personalize_by_location");
        jsonToDbMap.put("grok_permission_for_posts", "allow_posts_with_grok");
        jsonToDbMap.put("type_to_connect", "connected_acc_type");
        jsonToDbMap.put("account", "connected_acc");
        jsonToDbMap.put("pass", "password");
        jsonToDbMap.put("two_factor", "TFA");

        return jsonToDbMap;
    }
}
