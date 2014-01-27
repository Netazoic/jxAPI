package com.netazoic.jxapi;

import java.util.HashMap;
import java.util.Map;


public class jxAPI {

	public enum HTTP_Method{
		PUT,GET,POST
	}

	public enum JX_Action{
		rstat("rstat","RetrieveStatement"),
		bucket("bucket","Catch all action"), 
		statements("statements", "General statement action"),
		state("state", "General state action"), 
		saveStatement("saveStatement", "Save a statement"), 
		saveState("saveState","Save state"), 
		retrieveStatement("retrieveStatement","Retrieve a statement"), 
		retrieveState("retrieveState","Retreive state"), 
		retrieveActivityProfile("retrieveActivityProfile","Retrieve activity profile"),
		saveActivityProfile("saveActivityProfile","Save activity profile"),
		retrieveAgentProfile("retrieveAgentProfile","Retrieve agent profile"),
		saveAgentProfile("saveAgentProfile","Save agent profile"),
		queryStatements("queryStatements", "Query statements"), 
		retrieveVoided("retrieveVoided","Retrieve voided statement")
		;
	
		public String actionString;
		public String desc;
	
		JX_Action(String p, String d){
			actionString = p;
			desc = d;
		}
	}

	public enum X_API{
		Statement("statements"),
		State("activities/state"),
		ActivityProfile("activities/profile"),
		AgentProfile("agent/profile");
	
		String endpoint;
		Map<HTTP_Method,JX_Action> apiAction;
		X_API(String ep){
			endpoint = ep;
		}
		static{
			Map<HTTP_Method,JX_Action> map = new HashMap<HTTP_Method,JX_Action>();
			Statement.apiAction = new HashMap<HTTP_Method,JX_Action>();
			Statement.apiAction.put(HTTP_Method.GET, JX_Action.retrieveStatement);
			Statement.apiAction.put(HTTP_Method.PUT, JX_Action.saveStatement);
			Statement.apiAction.put(HTTP_Method.POST, JX_Action.saveStatement);
	
			State.apiAction = new HashMap<HTTP_Method,JX_Action>();
			State.apiAction.put(HTTP_Method.GET, JX_Action.retrieveState);
			State.apiAction.put(HTTP_Method.PUT, JX_Action.saveState);
			State.apiAction.put(HTTP_Method.POST, JX_Action.saveState);
	
			ActivityProfile.apiAction = new HashMap<HTTP_Method,JX_Action>();
			ActivityProfile.apiAction.put(HTTP_Method.GET, JX_Action.retrieveActivityProfile);
			ActivityProfile.apiAction.put(HTTP_Method.PUT, JX_Action.saveActivityProfile);
			ActivityProfile.apiAction.put(HTTP_Method.POST, JX_Action.saveActivityProfile);
	
			AgentProfile.apiAction = new HashMap<HTTP_Method,JX_Action>();
			AgentProfile.apiAction.put(HTTP_Method.GET, JX_Action.retrieveAgentProfile);
			AgentProfile.apiAction.put(HTTP_Method.PUT, JX_Action.saveAgentProfile);
			AgentProfile.apiAction.put(HTTP_Method.POST, JX_Action.saveAgentProfile);
	
		}
	}

}
