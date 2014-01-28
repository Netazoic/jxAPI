package com.netazoic.jxapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import lombok.extern.java.Log;

import org.apache.commons.net.ntp.TimeStamp;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netazoic.ent.NetAction;
import com.netazoic.ent.ServENT;
import com.netazoic.jxapi.Object.OBJ_Type;
import com.netazoic.jxapi.jxAPI.HTTP_Method;
import com.netazoic.jxapi.jxAPI.JX_Action;
import com.netazoic.jxapi.jxAPI.X_API;
import com.rusticisoftware.tincan.Agent;
import com.rusticisoftware.tincan.LRS;
import com.rusticisoftware.tincan.LanguageMap;
import com.rusticisoftware.tincan.RemoteLRS;
import com.rusticisoftware.tincan.State;
import com.rusticisoftware.tincan.Statement;
import com.rusticisoftware.tincan.StatementTarget;
import com.rusticisoftware.tincan.StatementsQueryInterface;
import com.rusticisoftware.tincan.StatementsResult;
/*
 * A remote LRS wrapper that incorporates a servlet for handling LRS requests
 */
import com.rusticisoftware.tincan.TCAPIVersion;
import com.rusticisoftware.tincan.Verb;
import com.rusticisoftware.tincan.exceptions.UnexpectedHTTPResponse;
import com.rusticisoftware.tincan.http.HTTPRequest;
import com.rusticisoftware.tincan.http.HTTPResponse;
import com.rusticisoftware.tincan.v10x.StatementsQuery;

@Log
public class jxLRS extends ServENT implements LRS {

	RemoteLRS rlrs;

	public enum LRS_Param{
		statementId, LRS_Endpoint, LRS_Username, LRS_Password, 
		object, actor, verb, timeFrom, activityId, profileId
	}
	StatementHandler sttmntHdlr = new StatementHandler();
	StateHandler stateHdlr = new StateHandler();
	ActivityProfileHandler activityHdlr = new ActivityProfileHandler();
	AgentProfileHandler agentHdlr = new AgentProfileHandler();

	public void init(ServletConfig config) throws javax.servlet.ServletException {
		super.init(config);
		TCAPIVersion ver = TCAPIVersion.V100;

		defaultAction = JX_Action.bucket.actionString;
		actionMap.put(JX_Action.bucket.actionString, sttmntHdlr);
		actionMap.put(JX_Action.statements.actionString, sttmntHdlr);
		actionMap.put(JX_Action.state.actionString, stateHdlr);
		actionMap.put(JX_Action.retrieveActivityProfile.actionString,activityHdlr);
		actionMap.put(JX_Action.retrieveAgentProfile.actionString, agentHdlr);
		actionMap.put(JX_Action.retrieveState.actionString, stateHdlr);
		actionMap.put(JX_Action.retrieveStatement.actionString, sttmntHdlr);
		actionMap.put(JX_Action.saveActivityProfile.actionString, activityHdlr);
		actionMap.put(JX_Action.saveAgentProfile.actionString, agentHdlr);
		actionMap.put(JX_Action.saveState.actionString, stateHdlr);
		actionMap.put(JX_Action.saveStatement.actionString, sttmntHdlr);
		try {
			rlrs =  getRemoteLRS(ver);
		} catch (Exception ex) {
			throw new ServletException(ex);
		}
	}

	//Service the request
	@Override
	public void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException{
		request.setCharacterEncoding("utf-8");
		Enumeration<String> params = request.getParameterNames();
		doParamParsing(request, params);
		HttpSession session = request.getSession();
		JX_Action jxAction = this.geNetAction(request);
		request.setAttribute(ENT_Param.actionString.name(), jxAction.actionString);
		NetAction action = (NetAction)actionMap.get(jxAction.actionString);
		if(action==null) action = (NetAction)actionMap.get(defaultAction);
		if (action!=null){
			Connection con = null;
			try{
				con = getConnection();
				//Do stuff here
				action.doAction(request, response, session);
			} 
			catch (Exception ex) {
				throw new ServletException(ex);
			}
			finally {
				if (con != null) 
					try {con.close();} catch (SQLException e) {}
			}
		}

		else {
			// no handler for the requested action
			throw new ServletException("Invalid Request!");
		}
	}

	private void ajaxResponse(String json, HttpServletResponse response)
			throws IOException {
		response.setContentType("text/xml");
		response.setHeader("Cache-Control", "no-cache");
		response.getWriter().write(json);
	}

	public JX_Action geNetAction(HttpServletRequest request) {
		String uri = request.getRequestURI();
		/*
		 * Expected format of the URI is:
		 *   <app_name>/jx/<api_name>[/<api_specifier>]
		 *   
		 *   api_names can be: 
		 *   		statements			== Statement
		 *   						PUT: saveStatement
		 *   						POST: saveStatement or saveStatements 
		 *   						GET: retrieveStatement or retrieveStatements
		 *   		activities/state  	== State
		 *   						PUT,POST = saveState
		 *   						GET: = retrieveState
		 *   						DELETE: deleteState
		 *   		activities/profile 	== Activity Profile
		 *   						PUT, POST: saveActivityProfile
		 *   						GET: retrieveActivityProfile
		 *   						DELETE: deleteActivityProfile
		 *   		agent/profile 		== Agent Profile
		 *   						PUT, POST: saveAgentProfile
		 *   						GET: retrieveAgentProfile
		 *   						DELETE: deleteAgentProfile
		 */
		//Action is the last part of the uri, after the last "/" and before any query
		// string or # locator
		String[] parts = uri.split("/");
		String appName = parts[0];
		String servletURL = parts[1];
		String api1,api2;
		api1 = parts[2];
		api2 = parts.length==4?parts[3]:null;
		String endPoint = api1;
		if(api2 != null) endPoint += "/" + api2;
		jxAPI.X_API api = null;
		for(X_API a : X_API.values()){
			if(a.endpoint.equals(endPoint)){
				api = a;
				continue;
			}
		}
		HTTP_Method method = HTTP_Method.valueOf(request.getMethod());
		JX_Action action = api.apiAction.get(method);
		return action;	
	}

	private Agent getAgent(JsonNode js) {
		Agent agent = Agent.fromJson(js);
		return agent;
	}

	private RemoteLRS getRemoteLRS(TCAPIVersion version) throws Exception {
		RemoteLRS obj = new RemoteLRS();
		if(version == null) version = TCAPIVersion.V100;
		obj.setEndpoint(getSetting(LRS_Param.LRS_Endpoint.name()));
		obj.setVersion(version);
		obj.setUsername(getSetting(LRS_Param.LRS_Username.name()));
		obj.setPassword(getSetting(LRS_Param.LRS_Password.name()));

		return obj;
	}
	private Verb getVerb(JsonNode js) throws URISyntaxException {
		//Verb  verb = new Verb();
		Verb verb = new Verb();
		String languageCode = "en-US";		//TODO -- make this dynamic
		String id = js.get("id").textValue();
		LanguageMap map = new LanguageMap();
		verb.setId(id);
		JsonNode jsmap = js.get("display");
		Iterator<String> it = jsmap.fieldNames();
		while(it.hasNext()){
			String f = it.next();
			map.put(f, verb.getId().toString());
			map.put(languageCode, jsmap.get(f).textValue());			
		}
		verb.setDisplay(map); 
		/*LanguageMap display = new LanguageMap();
		display.put("und", verb.getId().toString());
		display.put("en-US", "attempted");
		 */

		return verb;
	}
	private StatementTarget getXObject(JsonNode js) throws Exception {
		OBJ_Type objType = OBJ_Type.valueOf(js.get("objectType").textValue());
		switch(objType){
		case Activity:
			return new Activity(js);

		default:
			throw new Exception( "Unknown object type");
		}

	}

	private Activity mockActivity(String suffix) throws URISyntaxException {
		return new Activity("http://tincanapi.com/TinCanJava/Test/RemoteLRSTest_mockActivity/" + suffix);
	}
	private Agent mockAgent() {
		Agent obj = new Agent();
		obj.setMbox("mailto:tincanjava-test-tincan@tincanapi.com");

		return obj;
	}
	private Verb mockVerb() throws URISyntaxException {
		return new Verb("http://adlnet.gov/expapi/verbs/attempted");
	}
	private Verb mockVerbDisplay() throws URISyntaxException {
		Verb obj = mockVerb();
		LanguageMap display = new LanguageMap();
		display.put("und", obj.getId().toString());
		display.put("en-US", "attempted");

		obj.setDisplay(display);

		return obj;
	}

	@Override
	public StatementsResult moreStatements(String moreURL) throws Exception {
		return rlrs.moreStatements(moreURL);
	}

	public static JsonNode putToJSON(HttpServletRequest req) throws JsonProcessingException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		String data = br.readLine();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode obj = mapper.readTree(sb.toString());
		return obj;
	}

	@Override
	public StatementsResult queryStatements(StatementsQueryInterface query)
			throws Exception {
		return rlrs.queryStatements(query);
	}

	public HTTPResponse retrieveActivityProfile(String activityID, String profileID, TimeStamp since) throws Exception {
		HashMap<String,String> params = new HashMap<String,String>();
		params.put(LRS_Param.activityId.name(), activityID);
		if(profileID != null) params.put(LRS_Param.profileId.name(), profileID);
		if(since != null) params.put("since", since.toDateString());

		String queryString = "?";
		Boolean first = true;
		for(Map.Entry<String,String> parameter : params.entrySet()) {
			queryString += (first ? "" : "&") + URLEncoder.encode(parameter.getKey(), "UTF-8") + "=" + URLEncoder.encode(parameter.getValue(), "UTF-8").replace("+", "%20");
			first = false;
		}
		String apiString;
		if(profileID == null) apiString = "activities";
		else apiString = "activities/profile";

		HTTPRequest request = new HTTPRequest();
		URL endPoint = rlrs.getEndpoint();
		request.setURL(endPoint + apiString + queryString);

		HTTPResponse response = rlrs.sendRequest(request);
		int status = response.getStatus();

		if (status == 200) {
			return response;
		}
		else if (status == 404) {
			return null;
		}
		throw new UnexpectedHTTPResponse(response);
	}

	@Override
	public State retrieveState(String id, String activityId, Agent agent,
			UUID registration) throws Exception {
		State st = rlrs.retrieveState(id, activityId, agent, registration);
		return st;
	}
	@Override
	public Statement retrieveStatement(String id) throws Exception {
		return rlrs.retrieveStatement(id);
	}

	@Override
	public Statement retrieveVoidedStatement(String id) throws Exception {
		return rlrs.retrieveVoidedStatement(id);
	}

	@Override
	public UUID saveStatement(Statement statement) throws Exception {
		rlrs.saveStatement(statement);
		return statement.getId();
	}
	
	public void saveActivityProfile(String activityID, String profileID, String jsonVal) throws Exception {
		HashMap<String,String> params = new HashMap<String,String>();
		params.put(LRS_Param.activityId.name(), activityID);
		if(profileID != null) params.put(LRS_Param.profileId.name(), profileID);

		String queryString = "?";
		Boolean first = true;
		for(Map.Entry<String,String> parameter : params.entrySet()) {
			queryString += (first ? "" : "&") + URLEncoder.encode(parameter.getKey(), "UTF-8") + "=" + URLEncoder.encode(parameter.getValue(), "UTF-8").replace("+", "%20");
			first = false;
		}
		String apiString;
		if(profileID == null) apiString = "activities";
		else apiString = "activities/profile";
		
		//Get the current entity ETag
		HTTPResponse respCurrent = retrieveActivityProfile(activityID,profileID,null);
		String eTag = respCurrent.getHeader("ETag");

		HTTPRequest request = new HTTPRequest();
        request.setMethod(HttpMethods.PUT);
        request.addRequestHeader("If-Match", eTag);
		URL endPoint = rlrs.getEndpoint();
		request.setURL(endPoint + apiString + queryString);
		request.setRequestContent(new ByteArrayBuffer(jsonVal));

		HTTPResponse response = rlrs.sendRequest(request);
		int status = response.getStatus();

		if (status == 204) {
			//expected result
			//do nada
		}
		//return status of 409 means you didn't provide a valid ETag
		else throw new UnexpectedHTTPResponse(response);
		return;
	}

	@Override
	public List<String> saveStatements(List<Statement> statements)
			throws Exception {
		return rlrs.saveStatements(statements);
	}

	@Override
	public void saveState(State state, String activityId, Agent agent,
			UUID registration) throws Exception {
		rlrs.saveState(state, activityId, agent, registration);

	}

	public class StatementHandler extends ActionEO implements NetAction {

		public void action(HttpServletRequest request, HttpServletResponse response) throws Exception{
			String actionString = (String) request.getAttribute(ENT_Param.actionString.name());
			JX_Action jxAction = JX_Action.valueOf(actionString);
			switch(jxAction){
			case queryStatements:
				queryStatementsHdlr(request,response);
				break;
			case rstat:
			case retrieveStatement:
				retrieveStatementHdlr(request,response);
				break;
			case retrieveVoided:
				retrieveVoidedHdlr(request,response);
				break;
			case saveStatement:
				saveStatementHdlr(request,response);
				break;
			default:
				throw new Exception("Unexpected action");

			}
		}


		private void queryStatementsHdlr(HttpServletRequest request, HttpServletResponse response) throws Exception {
			StatementsQuery query = new StatementsQuery();
			//TODO finish this 
			Enumeration<String> keys = request.getAttributeNames();
			ObjectMapper mapper = new ObjectMapper();
			//Agent
			String reqAgent = (String) request.getAttribute("agent");	
			if(reqAgent != null){
				JsonNode agent = mapper.readTree(reqAgent);
				query.setAgent(new Agent().fromJson(agent));
			}
			//Activity
			String activityID = (String) request.getAttribute("activityId");
			if(activityID != null){
				URI uri = new URI(activityID);
				query.setActivityID(uri);
			}
			String since = (String) request.getAttribute("since");
	        if(since != null) query.setSince(new DateTime(since));
	        query.setLimit(10);

			StatementsResult result = rlrs.queryStatements(query);
			log.info(result.toJSON(true));
			ajaxResponse(result.toJSON(),response);
		}

		private void retrieveStatementHdlr(HttpServletRequest request, HttpServletResponse response)
				throws JsonProcessingException, IOException, URISyntaxException, Exception {
			String statementID = (String)request.getAttribute(LRS_Param.statementId.name());
			if(statementID == null){
				queryStatementsHdlr(request,response);
				return;
			}
			//TODO: Check id to make sure it is a valid UUID;
			Statement stat = retrieveStatement(statementID);
			ajaxResponse(stat.toJSON(), response);
		}
		

		private void retrieveVoidedHdlr(HttpServletRequest request, HttpServletResponse response) throws Exception {
			String statementID = (String) request.getAttribute(LRS_Param.statementId.name());
			Statement st = retrieveStatement(statementID);
			ajaxResponse(st.toJSON(),response);
		}


		private void saveStatementHdlr(HttpServletRequest request, HttpServletResponse response) throws Exception {
			Statement st = statementFromRequest(request);
			st.stamp(); // triggers a PUT
			saveStatement(st);
			ajaxResponse("{'good':'job'}",response);
		}

		private Statement statementFromRequest(HttpServletRequest request)
				throws JsonProcessingException, IOException,
				URISyntaxException, Exception {
			String statementID = (String)request.getAttribute(LRS_Param.statementId.name());
			//TODO: Check id to make sure it is a valid UUID;
			JsonNode jsPut = putToJSON(request);
			Statement st = new Statement(jsPut);
			JsonNode object = jsPut.get(LRS_Param.object.name());
			st.setActor(getAgent(jsPut.get(LRS_Param.actor.name())));
			st.setVerb(getVerb(jsPut.get(LRS_Param.verb.name())));
			st.setObject(getXObject(object));
			return st;
		}

	}


	public class StateHandler extends ActionEO implements NetAction {

		public void action(HttpServletRequest request, HttpServletResponse response) throws Exception{
			String actionString = (String) request.getAttribute(ENT_Param.actionString.name());
			JX_Action jxAction = JX_Action.valueOf(actionString);
			switch(jxAction){
			case retrieveState:
				retrieveStateHdlr(request,response);
				break;
			case saveState:
				saveStateHdlr(request,response);
				break;
			case state:
				saveStateHdlr(request,response);
				break;
			default:
				throw new Exception("Unexpected action");

			}
		}



		private void retrieveStateHdlr(HttpServletRequest request, HttpServletResponse response) {
			// TODO Auto-generated method stub

		}



		private void saveStateHdlr(HttpServletRequest request, HttpServletResponse response) {
			// TODO Auto-generated method stub

		}
	}


	public class ActivityProfileHandler extends ActionEO implements NetAction {

		public void action(HttpServletRequest request, HttpServletResponse response) throws Exception{
			String actionString = (String) request.getAttribute(ENT_Param.actionString.name());
			JX_Action jxAction = JX_Action.valueOf(actionString);
			switch(jxAction){

			case retrieveActivityProfile:
				retrieveActivityProfileHdlr(request,response);
				break;
			case saveActivityProfile:
				saveActivityProfileHdlr(request,response);
				break;
			default:
				throw new Exception("Unexpected action");
			}
		}
		private Activity activityFromRequest(HttpServletRequest request)
				throws JsonProcessingException, IOException,
				URISyntaxException, Exception {
			String activityID = (String)request.getAttribute(LRS_Param.activityId.name());
			Activity act = new Activity(activityID);
			return act;
		}



		private void saveActivityProfileHdlr(HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException, IOException, URISyntaxException, Exception {
			Activity act = activityFromRequest(request);
			String profileID = (String)request.getAttribute(LRS_Param.profileId.name());
			String id = act.getId().toString();
			JsonNode jsPut = putToJSON(request);
			String jsonVal = jsPut.toString();
			saveActivityProfile(id,profileID,jsonVal);
			ajaxResponse("{'good':'job'}",response);

		}

		private void retrieveActivityProfileHdlr(HttpServletRequest request, HttpServletResponse response) throws URISyntaxException, Exception {
			Activity act = activityFromRequest(request);
			String profileID = (String)request.getAttribute(LRS_Param.profileId.name());
			String id = act.getId().toString();
			//String profileContent = retrieveActivityProfile(id,profileID,null);
			HTTPResponse resp = retrieveActivityProfile(id,profileID,null);
			String profileContent = resp.getContent();
			if(profileContent == null) profileContent = "[]";
			ajaxResponse(profileContent,response);

		}
	}


	public class AgentProfileHandler extends ActionEO implements NetAction {

		public void action(HttpServletRequest request, HttpServletResponse response) throws Exception{
			String actionString = (String) request.getAttribute(ENT_Param.actionString.name());
			JX_Action jxAction = JX_Action.valueOf(actionString);
			switch(jxAction){
			case retrieveAgentProfile:
				retrieveAgentProfileHdlr(request,response);
				break;
			case saveAgentProfile:
				saveAgentProfileHdlr(request,response);
				break;
			default:
				throw new Exception("Unexpected action");
			}
		}

		private Agent agentFromRequest(HttpServletRequest request)
				throws JsonProcessingException, IOException,
				URISyntaxException, Exception {
			String statementID = (String)request.getAttribute(LRS_Param.statementId.name());
			//TODO: Check id to make sure it is a valid UUID;
			JsonNode jsPut = putToJSON(request);
			Statement st = new Statement(jsPut);
			Agent agent = new Agent();
			JsonNode object = jsPut.get(LRS_Param.object.name());
			st.setActor(getAgent(jsPut.get(LRS_Param.actor.name())));
			st.setVerb(getVerb(jsPut.get(LRS_Param.verb.name())));
			st.setObject(getXObject(object));
			return agent;
		}

		private void saveAgentProfileHdlr(HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException, IOException, URISyntaxException, Exception {
			//TODO finish me
			Agent agent = agentFromRequest(request);

		}

		private void retrieveAgentProfileHdlr(HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException, IOException, URISyntaxException, Exception {
			// TODO Auto-generated method stub
			Agent agent = agentFromRequest(request);


		}


	}


}
