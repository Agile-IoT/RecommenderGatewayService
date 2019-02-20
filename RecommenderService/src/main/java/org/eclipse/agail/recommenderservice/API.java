/*********************************************************************
 * Copyright (C) 2017 TUGraz.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.agail.recommenderservice;


import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.agail.recommenderservice.devAPImodels.AgileDevice;
import org.eclipse.agail.recommenderservice.devAPImodels.AgileWorkflowModel;
import org.eclipse.agail.recommenderservice.devAPImodels.DataModel;
import org.eclipse.agail.recommenderservice.devAPImodels.TokenModel;
import org.eclipse.agail.recommenderservice.recommendermodels.Device;
import org.eclipse.agail.recommenderservice.recommendermodels.GatewayProfile;
import org.eclipse.agail.recommenderservice.recommendermodels.ListOfClouds;
import org.eclipse.agail.recommenderservice.recommendermodels.ListOfDevices;
import org.eclipse.agail.recommenderservice.recommendermodels.ListOfWFs;
import org.eclipse.agail.recommenderservice.recommendermodels.Workflow;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;


@RestController
@EnableAutoConfiguration
@SpringBootApplication
public class API {
	
	static boolean flag;
	static ListOfWFs recommended_WFs = new ListOfWFs();
	static ListOfClouds recommended_Clouds = new ListOfClouds();
	static ListOfDevices recommended_Devices = new ListOfDevices();
	
	public static GatewayProfile recommenderProfile = new GatewayProfile(); 
	 
	public static String recommenderServerIP = "http://agile.ist.tugraz.at:8080/Recommender/";
	
	public static void main(String[] args) throws Exception {
	    SpringApplication.run(API.class, args);
	    flag = true;
	    
    	while (true) {
    	      // ------- code for task to run
    		  if(flag){
    			    updateProfile();
    		        getWFRecommendations();
    		        getCloudRecommendations();
    		        getDeviceRecommendations();
    		        flag=false;
    		  }
    	      System.out.println("Hello !!");
    	      // ------- ends here
    	   try {
    	       Thread.sleep(60000); // 1 minute
    	   } catch (InterruptedException e) {
    	        e.printStackTrace();
    	    }
        }
    
           
	}

    
	
	public static void updateProfile(){
	
		System.out.println("This part adds devices and workflows into the profile");
		try{
			// DEVICES
			recommenderProfile.devices = new ListOfDevices();
			
			RestTemplate restTemplate = new RestTemplate();
		    final String uri = "http://172.18.0.1:8080/api/devices";
		    AgileDevice [] devices = restTemplate.getForObject(uri, AgileDevice[].class);
		    
		    System.out.println("devices are taken");
		    System.out.println("devices lenght: "+devices.length);
		    System.out.println("first device's name: "+devices[0].getName());
		    
	    	if(devices!=null)
			    for(int i=0;i<devices.length;i++){
			    	Device dev = new Device();
			    	String devTitle="";
			    	devTitle += devices[i].getName().trim();
			    	for(int j=0;j<devices[i].getStreams().length;j++){
			    		devTitle += " ";
			    		devTitle += devices[i].getStreams()[j].getId().trim();
			    	} 
			    	
			    	dev.setTitle(devTitle);
			    	recommenderProfile.devices = new ListOfDevices();
			    	recommenderProfile.devices.addDevice(dev);
			    	
			    	System.out.println("Devices in the Profile: " + devTitle);
			    }
	    	
		}catch(Exception e){
			System.out.println("Exception in devices api: " + e.getMessage());
		}
		
		try{
			// WORKFLOW
			recommenderProfile.wfs = new ListOfWFs();
			RestTemplate restTemplate = new RestTemplate();
			
			// 1- GET TOKEN

			//  curl --data "client_id=node-red-admin&grant_type=password&scope=*&username=admin&password=password" http://172.18.0.1:1880/red/auth/token
			System.out.println("will call get token");
			
			String uri2 = "http://172.18.0.1:1880/red/auth/token";

			//  curl --data "client_id=node-red-admin&grant_type=password&scope=*&username=admin&password=password" http://agile-nodered:1880/red/auth/token
		    //String uri2 = "http://agile-nodered:1880/red/auth/token";

		    DataModel data = new DataModel();
		    TokenModel token = restTemplate.postForObject(uri2, data, TokenModel.class);
		    System.out.println("get token succeeded");
		    
		    // 2- SET TOKEN
		    // curl -H "Authorization: Bearer AEqPo4CKKr7j1CMUeqou7EuzjceeI6n4YPGcRd6XIQ3PJmBsXhyHjgX873z9J7ZoRjwU5YWPA7NBTdbGJNSWzt64K1z1nepPThS4EOFZZAZYBXX2aD4HvPjIJjlrr210" http://172.18.0.6:1880/red/settings

		    // System.out.println("will call settings");
		    
		    String uri3 = "http://172.18.0.1:1880/red/settings";

		    //String uri3 = "http://agile-nodered:1880/red/settings";

		    restTemplate = new RestTemplate();
		    HttpHeaders headers = new HttpHeaders();
		    headers.setContentType(MediaType.APPLICATION_JSON);
		    headers.set("Authorization", "Bearer "+token.getAccess_token() );
		    
		    HttpEntity<?> entity = new HttpEntity(headers);
		    // String result = restTemplate.postForObject(uri3, entity, String.class);
		    // System.out.println("settings succeeded");
		    
		    // 3- GET FLOWS

		    System.out.println("will call flows");
		    String uri4 = "http://172.18.0.1:1880/red/flows";

		    //String uri4 = "http://agile-nodered:1880/red/flows";

		    AgileWorkflowModel wfs = restTemplate.postForObject(uri4, entity, AgileWorkflowModel.class);
		    System.out.println("flows succeeded");
		    
		    
		    List<Workflow> wfList = new ArrayList<Workflow>();
	    	
		    for(int i=0;i<wfs.getV1().length;i++){
		    	Workflow wf = new Workflow();
		    	String title = wfs.getV1()[i].getType();
		    	if(checkTitle(title)){
			    	wf.setDatatag(title);
			    	wfList.add(wf);
		    	}
		    }
		    for(int i=0;i<wfs.getV2().getFlows().length;i++){
		    	Workflow wf = new Workflow();
		    	String title = wfs.getV2().getFlows()[i].getType();
		    	if(checkTitle(title)){
			    	wf.setDatatag(title);
			    	wfList.add(wf);
			    	System.out.println("Workflow in the Profile: " + title);
		    	}
		    }
		   
		    recommenderProfile.wfs.setWfList(wfList);
		    
		}catch(Exception e){
			System.out.println("Exception in nodered api: " + e.getMessage());
		}
		
		System.out.println("End of adding devices and workflows into the profile");
		
	}
	
	
	static boolean checkTitle(String title){
		boolean flag = false;
		
		// remove commen nodes in search
		if (title.contains("function") || 
				title.contains("switch") || 
				title.contains("debug") || 
				title.contains("template") || 
				title.contains("inject") ||
				title.contains("catch") ||
				title.contains("status") ||
				title.contains("delay") ||
				title.contains("trigger") ||
				title.contains("comment") ||
				title.contains("trigger") 
				){
			
			flag = true;
		}
		return flag;
	}

	
	@ModelAttribute
	public void setResponseHeader(HttpServletResponse response) {
	    response.addHeader("Access-Control-Allow-Origin", "*");
	}  
	
	
	/**
	 * @apiDescription AGILE Recommender and Configurator Docker Service
	 * @apiVersion 1.0.0
	 * 
	 */
    @RequestMapping("/")
    @ResponseBody
    public String home() {
        return "Agile Recommener and Configurator Service is running.";
    }
    
    private static void getWFRecommendations(){
    	
	    RestTemplate restTemplate = new RestTemplate();
	    final String uri = recommenderServerIP+"getWorkflowRecommendation";
			
	    System.out.println("2- call the service");
	    System.out.println("with recommenderProfile with dev size: "+recommenderProfile.getDevices().getDeviceList().size());
	    if(recommenderProfile.getDevices().getDeviceList().size()>0 && recommenderProfile.getDevices().getDeviceList().get(0).getTitle()!=null)
	    	System.out.println("with recommenderProfile with dev title: "+recommenderProfile.getDevices().getDeviceList().get(0).getTitle());
	    	
	    ObjectMapper mapper = new ObjectMapper();
	    	
	    	try{
	    		System.out.println(mapper.writeValueAsString(recommenderProfile));	
	    		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
	    		recommended_WFs = restTemplate.postForObject(uri, recommenderProfile, ListOfWFs.class);
	    		System.out.println("2- call the service done");
	    	}catch(Exception e){
	    		System.out.println("2- call the service failed: "+e.getMessage());
	    	}
	    	
    }
    
    private static void getCloudRecommendations(){
    	RestTemplate restTemplate = new RestTemplate();
	    final String uri = recommenderServerIP+"getCloudRecommendation";
			 
	    recommended_Clouds = restTemplate.postForObject(uri, recommenderProfile, ListOfClouds.class);
    	
    }
    
    private static void getDeviceRecommendations(){
    	RestTemplate restTemplate = new RestTemplate();
	    final String uri = recommenderServerIP+"getDeviceRecommendation";
			 
	    recommended_Devices = restTemplate.postForObject(uri, recommenderProfile, ListOfDevices.class);
    	
    	
    }
    
    
    /// SERVICES
    
    @ResponseBody @RequestMapping("/getWorkflowRecommendation")
    public ListOfWFs getWorkflowRecommendation () {
    	flag = true;
		return recommended_WFs;
    }

    @ResponseBody @RequestMapping("/getCloudRecommendation")
    public ListOfClouds getCloudRecommendation () {
    	flag = true;
		return recommended_Clouds;
    }
    

    @ResponseBody @RequestMapping("/getDeviceRecommendation")
    public ListOfDevices getDeviceRecommendation () {
    	flag = true;
		return recommended_Devices;
    }

 
}



