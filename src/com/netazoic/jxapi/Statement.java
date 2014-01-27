package com.netazoic.jxapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.netazoic.jxapi.jxLRS.LRS_Param;
import com.netazoic.jxapi.iface.IF_Actor;
import com.netazoic.jxapi.iface.IF_Object;

/*
 * Java implementation of the Experience API (xAPI)
 * https://github.com/adlnet/xAPI-Spec
 * 
 * This api based on the 1.0.1 October 2013 xAPI
 *  
 * http://www.adlnet.gov/wp-content/uploads/2013/10/xAPI_v1.0.1-2013-10-01.pdf
 */

/*
 * Copyright 2014 Netazoic 
 * Licensed under the Apache License, Version 2.0 (the "License"). 
 * You may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */

public class Statement  implements IF_Object{

 public UUID statementID;
 public Long actorID;
 public Actor actor;
 public Verb verb;
 public Long objectID; //The object of the action
 public Object object;
 public Result result;
 public Context context;
 public Timestamp timeStamp;  //ISO8601 timestamp
 public Timestamp stored;  //ISO 8601 -- date this activity was stored in the LRS
 public Actor authority;
 public String version;
 public Attachment[] attachments;
 
 public void createStatment(){
	 
 }
 
 public void deleteStatement(){
 }
 
 public void updateENT(){
	 
 }
 
 public void retrieveENT(){
	 
 }


}
 
 
