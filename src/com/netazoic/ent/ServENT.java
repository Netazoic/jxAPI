package com.netazoic.ent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * References:
 * http://www.javaranch.com/journal/200601/JDBCConnectionPooling.html
 * http://www.cyberciti.biz/faq/howto-add-postgresql-user-account/
 * http://tomcat.apache.org/tomcat-6.0-doc/jndi-datasource-examples-howto.html#PostgreSQL
 */

public class ServENT extends HttpServlet {
	public Map<String, NetAction> actionMap;
	public String defaultAction;
	private DataSource dataSource = null;

	public enum ENT_Param{
		netAction, actionString, Settings, jndiDB;

	}


	public void init(ServletConfig config) throws javax.servlet.ServletException {
		super.init( config);
		ServletContext context;
		Map<Object, String> settings;

		// We need to create the ConnectionPool, ServerSettings, Codes,
		// Authenticator, and anything else we need here.
		context = config.getServletContext();
		actionMap = new HashMap<String,NetAction>();
		String jndiDB = null;
		synchronized (context) {
			settings = getSettings();
			if (settings == null) {
				context.log("Creating Settings.");
				settings = new HashMap<Object, String>();
				Enumeration<String> params = context.getInitParameterNames();
				Object temp;
				while (params.hasMoreElements()) {
					temp = params.nextElement();
					settings.put(temp, context.getInitParameter(temp.toString()));
				}
				putSettings(settings);
			}
			try {
				//JNDI data connector
				jndiDB = context.getInitParameter(ENT_Param.jndiDB.name());
				//if present, should be a string in the form "jdbc/<dbname>"
				// default
				//if(jndiDB == null) jndiDB = "postgres";
				if(jndiDB != null){
					// Look up the JNDI data source only once at init time
					InitialContext cxt = new InitialContext();
					dataSource = (DataSource) cxt.lookup( "java:/comp/env/" + jndiDB );
					if ( dataSource == null ) {
						throw new ServletException("Data source not found!");
					}
				}
			} catch (NamingException e) {
				//specified dbName not found in the tomcat server.xml file
				log("DBName not found in server.xml: " + jndiDB);
			}

		}

	}

	public void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException{
		request.setCharacterEncoding("utf-8");
		Enumeration<String> params = request.getParameterNames();
		doParamParsing(request, params);
		HttpSession session = request.getSession();
		NetAction action = determineAction(request);
		if (action!=null)
			try {
				action.doAction(request, response, session);
			} catch (Exception e) {
				throw new ServletException(e);
			}
		else {
			// no handler for the requested action
			throw new ServletException("Invalid Request!");
		}
	}

	public void ajaxResponse(String json, HttpServletResponse response)
			throws IOException {
		response.setContentType("text/xml");
		response.setHeader("Cache-Control", "no-cache");
		response.getWriter().write(json);
	}
	
	public NetAction determineAction(HttpServletRequest request) {
		String actionString = determineActionString(request);



		NetAction action = (NetAction)actionMap.get(actionString);
		if(action==null) action = (NetAction)actionMap.get(defaultAction);
		return action;
	}

	public String determineActionString(HttpServletRequest request) {
		String url = request.getRequestURI();
		//TODO make smarter to handle multi-segmented actions like action/sub-action
		
		//Action is the last part of the uri, after the last "/" and before any query
		// string or # locator
		Integer idxQ=0,idxP=0,idxE;
		if(url.matches("\\?")) idxQ = url.indexOf('?');
		if(url.matches("#")) idxP = url.indexOf('#');
		idxE = idxQ>0?idxQ:idxP>0?idxP:url.length();
		String actionString = url.substring(url.lastIndexOf('/')+1,idxE);

		if(actionString != null){
			request.setAttribute(ENT_Param.actionString.name(), actionString);
		}
		return actionString;
	}


	public void doParamParsing(HttpServletRequest request,
			Enumeration<String> params) {
		String paramName;
		// Load the parameters into the attributes.
		while (params.hasMoreElements()) {
			paramName = params.nextElement();
			if (request.getAttribute(paramName)==null){
				String[] vals = request.getParameterValues(paramName);
				//Handling of multi-valued form params
				if(vals.length == 1) request.setAttribute(paramName, vals[0]);
				else {
					String val = "";
					for(String s : vals){
						if(s.equals(""))continue;
						val += s;
						val += ":";
					}
					if(val.indexOf(":") > -1){
						//Strip any opening ':' junk
						while(val.indexOf(":") == 0){
							val = val.substring(1);
						}
						//Strip a trailing ':'
						val = val.substring(0,val.lastIndexOf(":"));
					}
					request.setAttribute(paramName, val);
				}
			}
		}
	}

	public Connection getConnection() throws SQLException {
		if(dataSource == null) return null;
		return dataSource.getConnection();
	}

	public Map<Object, String> getSettings() {
		return (Map<Object, String>)getServletContext().getAttribute(ENT_Param.Settings.name());
	}
	public String getSetting(String key){
		return getSettings().get(key);
	}

	public void putSettings(Map<Object,String>settings){
		getServletContext().setAttribute(ENT_Param.Settings.name(), settings);
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

	public abstract class ActionWithConnectionEO implements NetAction {

		public abstract void action(HttpServletRequest request, HttpServletResponse response) throws IOException, Exception;

		@Override
		public void doAction(HttpServletRequest request,
				HttpServletResponse response, HttpSession session)
						throws Exception {
			Connection con = null;
			try{
				con = getConnection();
				//Do stuff here
				action(request,response);
			} 
			catch (SQLException sqlException) {
				sqlException.printStackTrace();
			}
			finally {
				if (con != null) 
					try {con.close();} catch (SQLException e) {}
			}
		}
	}
}