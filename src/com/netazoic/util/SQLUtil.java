package com.netazoic.util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;



public class SQLUtil {
	
	public enum SQL_Param{
		UserName, DatabaseServer, Password, jdbcLogLevel
	}

	public static int execSQL(String q, Connection con) throws Exception{
		Statement myStat = null;
		int result = 0;
		try{
			myStat = con.createStatement();
			myStat.execute(q);
			result = myStat.getUpdateCount();
		}
		catch(SQLException sqlex){
			throw new SQLException(sqlex);
		}
		catch(Exception ex){
			throw new Exception(ex);
		}
		finally{
			if(myStat != null)try{myStat.close();myStat=null;}catch(SQLException sqlex){}
		}
		return result;
	}

	public synchronized static String execSQL(String myQuery,String key, Connection con) throws SQLException{

		Statement myStat = null;
		String result = null;
		ResultSet rs = null;
		try{
			myStat = con.createStatement();
			rs = myStat.executeQuery(myQuery);
			if(rs.next()){
				result = rs.getString(key);
			}
		}
		catch(SQLException sqlex){
			throw new SQLException(sqlex);
		}
		finally{
			if(myStat != null)try{myStat.close();myStat=null;}catch(SQLException sqlex){}
		}
		return result;
	}
	
	  public synchronized static ResultSet execSQL(String myQuery, Statement myStat) throws SQLException{

		    String strVal = null;
		    ResultSet rs = null;
		    try{
		      rs = myStat.executeQuery(myQuery);
		    }
		    catch(SQLException ex){
		      throw ex;
		    }
		    finally{
		      // don't close the statement		    
		    }
		    return rs;
		  }
		  
	
	
	public static Connection getConnection(String userName, String password, String connURL, String jdbcLogLevel) throws SQLException {

	    Connection conn = null;
	    try {
	    	Properties props = new Properties();
	    	props.setProperty("user", userName);
	    	props.setProperty("password", password);
	    	props.setProperty("logUnclosedConnections", "true");
	    	props.setProperty("logLevel", jdbcLogLevel);	
	    	//props.setProperty("standard_conforming_strings", "on");
	    	conn = DriverManager.getConnection(connURL,props);
	    } catch (SQLException sqlex) {
	      // TODO Auto-generated catch block
	      throw sqlex;
	    }
	    return conn;
	}
	
	 public static Connection getConnection(Map<String,String>map) throws  SQLException{
		    Connection conn;
		    String userName,password,jdbcLogLevel,connURL;
		    connURL = (String)map.get(SQL_Param.DatabaseServer.name());
		    userName = (String)map.get(SQL_Param.UserName.name());
		    password = (String)map.get(SQL_Param.Password.name());
		    jdbcLogLevel = (String)map.get(SQL_Param.jdbcLogLevel.name());
		    return getConnection(userName,password,connURL,jdbcLogLevel);
		  }

	  public static String parseQueryFile(Map<String,Object> settings, String path) throws Exception{
		String q = readFile(path);
		return parseQuery(settings, q);
	}

	public static String parseQuery(Map<String,Object> settings, String q) throws Exception{

		try {
			String key, val,token;
			Object valObj;
			for(Map.Entry<String,Object> entry: settings.entrySet()){
				key = entry.getKey();
				valObj = entry.getValue();
				val = valObj.toString();
				token = "/$" + key + ";/";
				q.replaceAll(token, val);
			}
		} catch (Exception ex) {
			throw ex;
		}
		return q;
	}

	static String readFile(String path) throws IOException{
		Charset charset = StandardCharsets.UTF_8;
		return readFile(path,charset);
	}

	static String readFile(String path, Charset encoding) 
			throws IOException 
			{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
			}

	public static String readLargeFile(File file) throws IOException{
		return FileUtils.readFileToString(file);
	}
	
	public static void releaseConnection(Connection con) throws SQLException{
		con.close();
		con = null;
	}
}
