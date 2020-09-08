package com.ecoui.webservice.securityrealm;

import com.google.gson.Gson;
import com.ecoui.webservice.util.JdbcHelper;
//import com.ecoui.webservice.securityrealm.ActiveDierctory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;


//import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import javax.naming.NamingEnumeration;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.sql2o.tools.NamedParameterStatement;

@Path("securityrealm")
public class UsersAndGroups {
    public UsersAndGroups() {
    }
    private static final Logger logger = Logger.getLogger(UsersAndGroups.class);
    private static ObjectName userEditor;
    public static final String JNDI_FACTORY = "weblogic.jndi.WLInitialContextFactory";
    public static final String MBEAN_SERVER = "weblogic.management.mbeanservers.domainruntime";
    public static final String JNDI_ROOT = "/jndi/";
    public static final String DEFAULT_PROTOCOL = "t3";
    public static final String PROTOCOL_PROVIDER_PACKAGES = "weblogic.management.remote";
    public static final String DOMAIN_MBEAN_NAME =
        "com.bea:Name=DomainRuntimeService,Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean";
    private static MBeanServerConnection mBeanServerConnection;
    private static ObjectName defaultAuthenticator;
    private static ObjectName[] authenticationProviders;
    private static String authenticatorName = "DefaultAuthenticator";
    private static JSONObject providersObj = new JSONObject();
    private static String adProviderName;
    private static HashMap<String,ObjectName> providerMap=new HashMap<String,ObjectName>();

    static Gson gson = new Gson();

    public static ConfigData configs() throws Exception {
        String username = "";
        String password = "";
        String imagingHost = "";
        String imagingPort = "";
        String adProvider="";
        String ADAdminUserName = "";
        String ADAdminPassword = "";
        String ADDomainController = "";
        String ADDCValue= "";
        String company= "";
        Connection ebsconn = JdbcHelper.getEcouiJDBCConn(true);

        ConfigData config = new ConfigData(username, password, imagingHost, imagingPort,adProvider,ADAdminUserName,ADAdminPassword,ADDomainController,ADDCValue,company);
        String query = "select * from ecoui_configs"; //xxcv_img_configs


        NamedParameterStatement p = new NamedParameterStatement(ebsconn, query, true);

        ResultSet rs = null;

        try {
            rs = p.executeQuery();

            while (rs.next()) {


                if (rs.getString("configs_name").equalsIgnoreCase("WeblogicHost"))
                    imagingHost = rs.getString("configs_value");

                if (rs.getString("configs_name").equalsIgnoreCase("WeblogicPort"))
                    imagingPort = rs.getString("configs_value");

                if (rs.getString("configs_name").equalsIgnoreCase("WeblogicUsername"))
                    username = rs.getString("configs_value");

                if (rs.getString("configs_name").equalsIgnoreCase("WeblogicPassword"))
                    password = rs.getString("configs_value");
                
                if (rs.getString("configs_name").equalsIgnoreCase("ADProvider"))
                    adProvider = rs.getString("configs_value");
                if (rs.getString("configs_name").equalsIgnoreCase("ADAdminUserName"))
                    ADAdminUserName = rs.getString("configs_value");
                if (rs.getString("configs_name").equalsIgnoreCase("ADAdminPassword"))
                    ADAdminPassword = rs.getString("configs_value");
                if (rs.getString("configs_name").equalsIgnoreCase("ADDomainController"))
                    ADDomainController = rs.getString("configs_value");
                if (rs.getString("configs_name").equalsIgnoreCase("ADDCValue"))
                    ADDCValue = rs.getString("configs_value");
                if (rs.getString("configs_name").equalsIgnoreCase("ENV"))
                    company = rs.getString("configs_value");
                System.out.println("UserName: "+username);
                
            }
            config = new ConfigData(username, password, imagingHost, imagingPort,adProvider,ADAdminUserName,ADAdminPassword,ADDomainController,ADDCValue,company);
            p.close();
            rs.close();
        } catch (SQLException e) {
            
           // System.out.println("Error in configs: "+e.getMessage());
        } finally {

            ebsconn.close();
        }


        return config;
    }

    static class ConfigData {
        String userName, password, imagingHost, imagingPort, adProvider , ADAdminUserName,ADAdminPassword,ADDomainController, ADDCValue,company;

        public ConfigData(String userName, String password, String imagingHost, String imagingPort, String adProvider ,String ADAdminUserName,String ADAdminPassword,String ADDomainController,String ADDCValue,String company) {
            this.userName = userName;
            this.password = password;
            this.imagingHost = imagingHost;
            this.imagingPort = imagingPort;
            this.adProvider = adProvider;
            this.ADAdminUserName = ADAdminUserName;
            this.ADAdminPassword = ADAdminPassword;
            this.ADDomainController = ADDomainController;
            this.ADDCValue= ADDCValue;
            this.company = company;

        }
    }

    static {
        try {
            ConfigData config = configs();
            String username   = config.userName;
            String password   = config.password;
            String host = config.imagingHost;
            String port = config.imagingPort;
            adProviderName = config.adProvider;
            logger.info("info passed to jmx server");
            logger.info("username: " + username +"  imagingHost: " + host+ "  imagingPort: "+ port+"  adProvider: "+adProviderName);
//            System.out.println(username);
//            System.out.println(password);
//            System.out.println(host);
//            System.out.println(port);
            
//               String host = "150.136.196.150";
//               String port = "7001";
//               String username = "weblogic";
//               String password = "weblogic1";
            
            Hashtable h = new Hashtable();
            JMXServiceURL serviceURL;
            serviceURL =
                new JMXServiceURL(DEFAULT_PROTOCOL, host, Integer.valueOf(port).intValue(),
                                  "/jndi/weblogic.management.mbeanservers.domainruntime");
            h.put("java.naming.security.principal", username);
            h.put("java.naming.security.credentials", password);
            h.put("jmx.remote.protocol.provider.pkgs", "weblogic.management.remote");
            //Creating a JMXConnector to connect to JMX
            JMXConnector connector = JMXConnectorFactory.connect(serviceURL, h);
            mBeanServerConnection = connector.getMBeanServerConnection();
            /****
                  We Get Objects by creating ObjectName with it's Qualified name.
                  The constructor take a String of the full Qualified name of the MBean
                  We then use mBeanServerConnection to get Attribute out of this ObjectName but specifying a String of
                  this Attribute
                  *****/
            ObjectName configurationMBeans = new ObjectName(DOMAIN_MBEAN_NAME);
            ObjectName domain = (ObjectName) mBeanServerConnection.getAttribute(configurationMBeans, "DomainConfiguration");
            ObjectName security = (ObjectName) mBeanServerConnection.getAttribute(domain, "SecurityConfiguration");
            ObjectName realm = (ObjectName) mBeanServerConnection.getAttribute(security, "DefaultRealm");
            authenticationProviders = (ObjectName[]) mBeanServerConnection.getAttribute(realm, "AuthenticationProviders");
            
            
            
            for (int i = 0; i < authenticationProviders.length; i++) {
                String name = (String) mBeanServerConnection.getAttribute(authenticationProviders[i], "Name");
             //   System.out.println("name: "+name);
             logger.info("providername: "+name);
                if (name.equals(authenticatorName) || name.equals(adProviderName))
                {
                    userEditor = authenticationProviders[i];
                    logger.info("userEditor: "+ userEditor);
                    providerMap.put(name, userEditor);
                    
                    
                }
            }
        } catch (Exception e) {
            System.out.println("Error in static");
            throw new RuntimeException(e);
        }
    }


    @GET
    @Path("/m2")
    public String getData() {
     //   System.out.println("Latest");
        // invoke();
        //  getListOfUsers();
        return "Success";
    }
    
    @GET
        @Path("/getListOfUsers")
        @Produces("application/json")
        public static Response getListOfUsers() throws RuntimeException {
            logger.info("userEditor in getlistofusers: "+ userEditor);
            try {
                List allUsers = new ArrayList();
                String cursor =
                    (String) mBeanServerConnection.invoke(userEditor, "listUsers",
                                                          new Object[] { "*", Integer.valueOf(9999) },
                                                          new String[] { "java.lang.String", "java.lang.Integer" });
                boolean haveCurrent =
                    ((Boolean) mBeanServerConnection.invoke(userEditor, "haveCurrent", new Object[] { cursor },
                                                            new String[] { "java.lang.String" })).booleanValue();
                while (haveCurrent) {
                    String currentName =
                        (String) mBeanServerConnection.invoke(userEditor, "getCurrentName", new Object[] { cursor },
                                                              new String[] { "java.lang.String" });
                    
                 //   System.out.println("User: " + currentName);
                    allUsers.add(currentName);
                    mBeanServerConnection.invoke(userEditor, "advance", new Object[] { cursor },
                                                 new String[] { "java.lang.String" });
                    haveCurrent =
                        ((Boolean) mBeanServerConnection.invoke(userEditor, "haveCurrent", new Object[] { cursor },
                                                                new String[] { "java.lang.String" })).booleanValue();
                }


                //  return allUsers;
                return Response.status(200)
                               .entity(gson.toJson(allUsers))
                               .header("Access-Control-Allow-Origin", "*")
                               .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                               .build();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @GET
        @Path("/getUserGroups")
        @Produces("application/json")
        public static Response getUserGroups(@QueryParam("username") String username) throws RuntimeException {
            try {
                List allUserGroups = new ArrayList();
                userEditor = providerMap.get(adProviderName);
                logger.info("AD userEditor in getUserGroups: "+ userEditor);
                String cursor =
                    (String) mBeanServerConnection.invoke(userEditor, "listMemberGroups", new Object[] { username },
                                                          new String[] { "java.lang.String" });
                boolean haveCurrent =
                    ((Boolean) mBeanServerConnection.invoke(userEditor, "haveCurrent", new Object[] { cursor },
                                                            new String[] { "java.lang.String" })).booleanValue();
                while (haveCurrent) {
                    String currentName =
                        (String) mBeanServerConnection.invoke(userEditor, "getCurrentName", new Object[] { cursor },
                                                              new String[] { "java.lang.String" });
                    allUserGroups.add(currentName);
                    mBeanServerConnection.invoke(userEditor, "advance", new Object[] { cursor },
                                                 new String[] { "java.lang.String" });
                    haveCurrent =
                        ((Boolean) mBeanServerConnection.invoke(userEditor, "haveCurrent", new Object[] { cursor },
                                                                new String[] { "java.lang.String" })).booleanValue();
                }
                return Response.status(200)
                               .entity(gson.toJson(allUserGroups))
                               .header("Access-Control-Allow-Origin", "*")
                               .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                               .build();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @GET
        @Path("/getGroupMembers")
        @Produces("application/json")
        public static Response getGroupMembers(@QueryParam("groupName") String groupName) throws RuntimeException {
            List allGroupMembers = new ArrayList();
            userEditor = providerMap.get(adProviderName);//Get the provider of ADProvider
            System.out.println("AD userEditor in getgroupmembers: "+ userEditor);
            if(isGroupExists(groupName,userEditor))//If group doesnt exist in ADProvider then pull the provider from DefaultAuthenticator
            {
                        
                 //   System.out.println("Provider: "+userEditor);
                  //  List allGroupMembers = new ArrayList();
                    try {
                        
                        String cursor =
                            (String) mBeanServerConnection.invoke(userEditor, "listGroupMembers",
                                                                  new Object[] { groupName, "*", new java.lang.Integer(0) },
                                                                  new String[] { "java.lang.String", "java.lang.String",
                                                                                 "java.lang.Integer" });
                        boolean haveCurrent =
                            ((Boolean) mBeanServerConnection.invoke(userEditor, "haveCurrent", new Object[] { cursor },
                                                                    new String[] { "java.lang.String" })).booleanValue();
                        while (haveCurrent) {
                            String currentName =
                                (String) mBeanServerConnection.invoke(userEditor, "getCurrentName", new Object[] { cursor },
                                                                      new String[] { "java.lang.String" });
                            
//                            String departmentNum =
//                                (String) mBeanServerConnection.invoke(userEditor, "getUserAttributeValue", new Object[] { "1899","departmentnumber" },
//                                                                      new String[] { "java.lang.String" });
                            
                            currentName = currentName;
                            allGroupMembers.add(currentName);
                            mBeanServerConnection.invoke(userEditor, "advance", new Object[] { cursor },
                                                         new String[] { "java.lang.String" });
                            haveCurrent =
                                ((Boolean) mBeanServerConnection.invoke(userEditor, "haveCurrent", new Object[] { cursor },
                                                                        new String[] { "java.lang.String" })).booleanValue();
                        }
                        
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
            }
            
//            userEditor = providerMap.get(authenticatorName); 
//            logger.info("default userEditor in getgroupmembers: "+ userEditor);
//            if(isGroupExists(groupName,userEditor))
//            {
//                        userEditor = providerMap.get(authenticatorName); 
//                 //   System.out.println("Provider: "+userEditor);
//                    
//                    try {
//                        
//                        String cursor =
//                            (String) mBeanServerConnection.invoke(userEditor, "listGroupMembers",
//                                                                  new Object[] { groupName, "*", new java.lang.Integer(0) },
//                                                                  new String[] { "java.lang.String", "java.lang.String",
//                                                                                 "java.lang.Integer" });
//                        boolean haveCurrent =
//                            ((Boolean) mBeanServerConnection.invoke(userEditor, "haveCurrent", new Object[] { cursor },
//                                                                    new String[] { "java.lang.String" })).booleanValue();
//                        while (haveCurrent) {
//                            String currentName =
//                                (String) mBeanServerConnection.invoke(userEditor, "getCurrentName", new Object[] { cursor },
//                                                                      new String[] { "java.lang.String" });
//                            currentName = currentName+"("+authenticatorName+")";
//                            allGroupMembers.add(currentName);
//                            mBeanServerConnection.invoke(userEditor, "advance", new Object[] { cursor },
//                                                         new String[] { "java.lang.String" });
//                            haveCurrent =
//                                ((Boolean) mBeanServerConnection.invoke(userEditor, "haveCurrent", new Object[] { cursor },
//                                                                        new String[] { "java.lang.String" })).booleanValue();
//                        }
//                        
//                    } catch (Exception ex) {
//                        throw new RuntimeException(ex);
//                    }
//            }
            
        //    return allGroupMembers;
            return Response.status(200)
                           .entity(gson.toJson(allGroupMembers))
                           .header("Access-Control-Allow-Origin", "*")
                           .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                           .build();
        }

        @GET
        @Path("/getListOfGroups")
        @Produces("application/json")
        public static Response getListOfGroups() throws RuntimeException {
            try {
                List allUsers = new ArrayList();
                String cursor =
                    (String) mBeanServerConnection.invoke(userEditor, "listGroups",
                                                          new Object[] { "*", Integer.valueOf(9999) },
                                                          new String[] { "java.lang.String", "java.lang.Integer" });
                boolean haveCurrent =
                    ((Boolean) mBeanServerConnection.invoke(userEditor, "haveCurrent", new Object[] { cursor },
                                                            new String[] { "java.lang.String" })).booleanValue();
                while (haveCurrent) {
                    String currentName =
                        (String) mBeanServerConnection.invoke(userEditor, "getCurrentName", new Object[] { cursor },
                                                              new String[] { "java.lang.String" });
                    allUsers.add(currentName);
                    mBeanServerConnection.invoke(userEditor, "advance", new Object[] { cursor },
                                                 new String[] { "java.lang.String" });
                    haveCurrent =
                        ((Boolean) mBeanServerConnection.invoke(userEditor, "haveCurrent", new Object[] { cursor },
                                                                new String[] { "java.lang.String" })).booleanValue();
                }
                return Response.status(200)
                               .entity(gson.toJson(allUsers))
                               .header("Access-Control-Allow-Origin", "*")
                               .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                               .build();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        
        @GET
        @Path("/getGroupsTheirUsers")
        @Produces("application/json")
        public static Response getGroupsTheirUsers() throws RuntimeException {
           // System.out.println("getGroupsTheirUsers Invoked");
            JSONArray jsonArray = new JSONArray();
            
            JSONObject groupObj = new JSONObject();
            JSONObject userObj = new JSONObject();
            try {
                List allUsers = new ArrayList();
                
                String cursor =
                    (String) mBeanServerConnection.invoke(userEditor, "listGroups",
                                                          new Object[] { "*", Integer.valueOf(9999) },
                                                          new String[] { "java.lang.String", "java.lang.Integer" });
                boolean haveCurrent =
                    ((Boolean) mBeanServerConnection.invoke(userEditor, "haveCurrent", new Object[] { cursor },
                                                            new String[] { "java.lang.String" })).booleanValue();
                while (haveCurrent) {
                    String currentName =
                        (String) mBeanServerConnection.invoke(userEditor, "getCurrentName", new Object[] { cursor },
                                                              new String[] { "java.lang.String" });
                    allUsers.add(currentName);
                    
                   // System.out.println("Group:"+currentName);
                    
                    if(currentName.contains("EMEA.HR"))
                    {
                      //  System.out.println("EMEA Group:"+currentName);
                        JSONObject finalobj = new JSONObject();
                        finalobj.put("group", currentName);
                        finalobj.put("users", getGroupMembers(currentName));
                        finalobj.put("read", "");
                        finalobj.put("write", "");
                       // finalobj.add(groupObj   );
                        jsonArray.put(finalobj);
                    }
                  //  jsonArray.put(userObj);
                    mBeanServerConnection.invoke(userEditor, "advance", new Object[] { cursor },
                                                 new String[] { "java.lang.String" });
                    haveCurrent =
                        ((Boolean) mBeanServerConnection.invoke(userEditor, "haveCurrent", new Object[] { cursor },
                                                                new String[] { "java.lang.String" })).booleanValue();
                }
                return Response.status(200)
                               .entity(jsonArray.toString())
                               .header("Access-Control-Allow-Origin", "*")
                               .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                               .build();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @POST
        @Path("/createUser")
        @Consumes(MediaType.APPLICATION_JSON)
        public static String createUser(String JsonPayload) {
            JSONParser parser = new JSONParser();
            try {
                org.json.simple.JSONObject preferenceObj;

                preferenceObj = (org.json.simple.JSONObject) parser.parse(JsonPayload);

                String username = (String) preferenceObj.get("userName");
                String psw = (String) preferenceObj.get("password");
                String desc = (String) preferenceObj.get("description");

                try {
                    mBeanServerConnection.invoke(userEditor, "createUser", new Object[] { username, psw, desc },
                                                 new String[] { "java.lang.String", "java.lang.String",
                                                                "java.lang.String" });
                    return username+" created successfully";
                } catch (Exception ex) {
                    ex.printStackTrace();

                }

            } catch (ParseException e) {
                return "Error while creating User";
            }
            return "Error while creating User";
        }

        public static boolean removeUser(String username) {
            try {
                if (!username.equalsIgnoreCase("weblogic")) {
                    mBeanServerConnection.invoke(userEditor, "removeUser", new Object[] { username },
                                                 new String[] { "java.lang.String" });
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        public static boolean resetUserPassword(String username, String newPassword) {
            try {
                if (!username.equalsIgnoreCase("weblogic")) {
                    mBeanServerConnection.invoke(userEditor, "resetUserPassword", new Object[] { username, newPassword },
                                                 new String[] { "java.lang.String", "java.lang.String" });
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        public static boolean isUserExists(String currentUser) throws RuntimeException {
            try {
                boolean userExists =
                    ((Boolean) mBeanServerConnection.invoke(userEditor, "userExists", new Object[] { currentUser },
                                                            new String[] { "java.lang.String" })).booleanValue();
                return userExists;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public static boolean isGroupExists(String currentGroup, ObjectName providerName) throws RuntimeException {
            try {
              //  userEditor = providersObj.get(adProviderName);
             
                  
                
                boolean gourpExists =
                    ((Boolean) mBeanServerConnection.invoke(providerName, "groupExists", new Object[] { currentGroup },
                                                            new String[] { "java.lang.String" })).booleanValue();
                return gourpExists;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    @GET
        @Path("/getUserAttributes")
        @Produces("application/json")
        public static Response getUserAttributes() throws RuntimeException {
        
            userEditor = providerMap.get(authenticatorName);//Get the provider of ADProvider
            
//            if(!isGroupExists(groupName,userEditor))//If group doesnt exist in ADProvider then pull the provider from DefaultAuthenticator
//                userEditor = providerMap.get(authenticatorName); 
        
            try {
                List allUsers = new ArrayList();
                
                    String currentName =
                        (String) mBeanServerConnection.invoke(userEditor, "getUserAttributeValue", new Object[] { "1899","employeenumber" },
                                                              new String[] { "java.lang.String" });
                    
                  //  System.out.println("UserAttribute: " + currentName);
                   
                


                //  return allUsers;
                return Response.status(200)
                               .entity(gson.toJson(allUsers))
                               .header("Access-Control-Allow-Origin", "*")
                               .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                               .build();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
   /* @GET
            @Path("/ADAttributes")
            @Produces("application/json")
            public static Response ADAttributes(@QueryParam("username") String username) throws RuntimeException, Exception {
             ConfigData config = configs();
            System.out.println(config.ADAdminUserName);
            System.out.println(config.ADAdminPassword);
            System.out.println(config.ADDomainController);
                ActiveDirectory activeDirectory = new ActiveDirectory(config.ADAdminUserName, config.ADAdminPassword, config.ADDomainController);
            try {
                NamingEnumeration<SearchResult> results =
                    activeDirectory.searchUser(username,"username",config.ADDCValue);
                SearchResult searchResult = null;
                List allUsers = new ArrayList();
                JSONObject jsonOutput = new JSONObject();
               
                while (results.hasMore()) {
                //    System.out.println("results has more elements");
                    SearchResult result = results.next();
                    Attributes attributes = result.getAttributes();
                    String value ="";
                   
                                for (NamingEnumeration ae = attributes.getAll(); ae.hasMore();)
                                {
                                    Attribute attr = (Attribute) ae.next();
                              //      System.out.println("Attribute ID: " + attr.getID());
                    
                                    // Print values of current attribute
                                    NamingEnumeration e = attr.getAll();
                                    while(e.hasMore())
                                    {
                                         value = e.next().toString();
                                   //     System.out.println("Value: " + value);
                                    }
                                   
                                    jsonOutput.put(attr.getID(), value);
                                }
                          
                   
                }
               
               
                               return Response.status(200)
                                   .entity(jsonOutput.toString())
                                   .header("Access-Control-Allow-Origin", "*")
                                   .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                                   .build();
            } catch (NamingException e) {
                return Response.status(200)
                               .entity(e.getMessage())
                               .header("Access-Control-Allow-Origin", "*")
                               .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                               .build();
            }
        } */   

}
