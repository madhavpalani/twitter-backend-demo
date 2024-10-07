package com.twitter.servlets.Servlets;

import com.twitter.servlets.DBconnection.DBUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class authFilter implements Filter {

    // Define a list of endpoints that should be excluded from authentication
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response; // Cast to HttpServletResponse
        String requestURI = httpRequest.getRequestURI();
        System.out.println(requestURI);

        // Skip authentication for excluded paths
        if (requestURI.startsWith("/demo2/users")) {
            chain.doFilter(request, response); // Continue to the next filter or servlet
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");
        List<String> authCredentials = getAuthCredentials(authHeader);

        if (validateUser(authCredentials)) {
            chain.doFilter(request, response); // Continue to the next filter or servlet
        } else {
            httpResponse.getWriter().write("Unauthorized"); // Use HttpServletResponse for writing response
            httpResponse.setStatus(400); // Set the response status
        }
    }

    @Override
    public void destroy() {}

    private List<String> getAuthCredentials(String authHeader) {
        List<String> auth = new ArrayList<>();
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            byte[] decoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decoded, StandardCharsets.UTF_8);
            final String[] values = credentials.split(":", 2);
            auth.add(values[0]);
            auth.add(values[1]);
        }
        return auth;
    }

    private boolean validateUser(List<String> credentials) {
        try(Connection connection= DBUtil.getConnection()){
            String retrieveIDQuery = "SELECT ua.user_id FROM user_auth ua " +
                    "JOIN user_details ud ON ua.user_id=ud.user_id " +
                    "WHERE ud.user_name = ? AND " +
                    "ua.password = ? ";
            try (PreparedStatement pt = connection.prepareStatement(retrieveIDQuery)) {
                pt.setString(1, credentials.get(0));
                pt.setString(2, credentials.get(1));
                ResultSet rs = pt.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

}
