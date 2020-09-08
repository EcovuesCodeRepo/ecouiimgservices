package com.ecoui.webservice;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("webservice")
@ApplicationPath("resources")
public class SampleController {
    public SampleController() {
    }

    @GET
    @Path("/validate")
    public String getData() {

       return "Rest WebService is working fine";
    }

   
}
