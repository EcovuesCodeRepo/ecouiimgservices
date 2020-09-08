package com.ecoui.webservice.securityrealm;

import com.sun.jersey.json.impl.writer.JsonEncoder;

import java.security.Principal;
import java.security.Principal;

import java.sql.Connection;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Set;

import javax.security.auth.Subject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

//import ecoui.rest.util.JdbcHelper;

import javax.ws.rs.QueryParam;

import org.json.JSONArray;
import org.json.JSONObject;

import org.sql2o.tools.NamedParameterStatement;



import weblogic.security.Security;
import weblogic.security.principal.WLSGroupImpl;
import weblogic.security.principal.WLSUserImpl;
import com.ecoui.webservice.util.JdbcHelper;
//import org.json.simple.JSONObject;

@Path("userroles")
public class UsersRolesRest {
   
    public UsersRolesRest() {
        super();
    }
    
    @GET
    @Path("/m1")
    public String getName() {
        return "Rest service working fine";
    }
    public String dbLink=JdbcHelper.dbLink;
    @GET
    @Path("/getUserName")
    @Produces("application/json")
    public Response getUserRoleInfo() {
    
            String userRoleInfo = "";
         ArrayList<String> roles = new ArrayList<String>();
             String user = null;
        
            Subject subject = Security.getCurrentSubject();    
    //        String username = weblogic.security.SubjectUtils.getUsername(
    //        weblogic.security.Security.getCurrentSubject());
        
        Set<Principal> allPrincipals = subject.getPrincipals();
                for (Principal principal : allPrincipals) {
                    if ( principal instanceof WLSGroupImpl ) {
                        System.out.println("found role: "+principal.getName());
                        roles.add(principal.getName());
                    }
                    if ( principal instanceof WLSUserImpl ) {
                        System.out.println("found user: "+principal.getName());
                        user = principal.getName();
                        
                    }            
                }  
    
            userRoleInfo = "{\"userName\": \"" + user + "\",\"userDisplayName\": \"" + user
                    + "\",\"role\": [" + getEnterpriseRole(subject) + "]}";
            
        
    
        return Response.status(200)
                                          .entity( userRoleInfo )
                                          .header("Access-Control-Allow-Origin", "*")
                                          .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
                                    //      .allow("OPTIONS").build();
      //  return userRoleInfo;
    }
    private String getEnterpriseRole(Subject mySubject) {
        String roleDisplayName = "";
        Set<java.security.Principal> principals = mySubject.getPrincipals();
        int principalSize = principals.size();
        int i = 1;
        for (java.security.Principal principal : principals) {
            if (i != principalSize) {
                if (principal.getClass() == WLSGroupImpl.class) {
                    roleDisplayName += "{\"display\":\"" + principal.getName() + "\"},";
                }
            } else {
                roleDisplayName += "{\"display\":\"" + principal.getName() + "\"}";
            }
            i++;
        }
        return roleDisplayName;
    }
    
    public static JSONObject convertToJSON(ResultSet resultSet)
            throws Exception {
        JSONArray jsonArray = new JSONArray();
        JSONObject finalobj = new JSONObject();
        while (resultSet.next()) {
            int total_rows = resultSet.getMetaData().getColumnCount();
            JSONObject obj = new JSONObject();
            for (int i = 0; i < total_rows; i++) {
               
                obj.put(resultSet.getMetaData().getColumnLabel(i + 1)
                        .toLowerCase(), resultSet.getObject(i + 1));
                
            }
            jsonArray.put(obj);
        }
        return finalobj.put("items",jsonArray);
            }   
    
    
    
    
  
    
    @GET
            @Path("/getOrdsUrl")
            public Response getOrdsUrl()throws Exception{
              //  System.out.println("Yes");
                JSONObject obj = new JSONObject();
                Connection conn = JdbcHelper.getJDBCConnectionFromDataSource(true);
                String ordsUrl="";
                            PreparedStatement ps = null;
                            ResultSet rs=null;
                            try {
                                     ps = conn.prepareStatement("SELECT configs_value ordsurl, (SELECT configs_value FROM ecoui_configs WHERE configs_name = 'client_credentials') client_values FROM ecoui_configs WHERE configs_name = 'baseURL'");
                                // ps.setLong(1,jobId);
                                 
                                  rs = ps.executeQuery();
                                 
                                 while(rs.next()) {
                                     
                                   ordsUrl = rs.getString("ordsurl");
                                     ordsUrl = rs.getString("ordsurl");
                                     
                                     obj.put("ordsUrl", rs.getString("ordsurl"));
                                     obj.put("clientValue", rs.getString("client_values"));
                                 }
                             } catch (SQLException e) {
                               //  System.out.println("Found error while executing query: "+e.toString());
                                 e.printStackTrace();
                                 return Response.status(200)
                                                         .entity( e.toString())
                                                         .header("Access-Control-Allow-Origin", "*")
                                                         .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
                             }finally{
                                 //connMgr.closeJDBCConnection(conn);
                                
                                 ps.close();
                                // System.out.println("Prepared Statement Closed");
                                 if(rs!=null)
                                 rs.close();
                               //  System.out.println("Resultset Closed");
                                 conn.close();
                                // System.out.println("Connection Closed");
                             }
                
            
                return Response.status(200)
                                        .entity( obj.toString())
                                        .header("Access-Control-Allow-Origin", "*")
                                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
            }
}
