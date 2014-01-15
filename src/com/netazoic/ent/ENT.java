package com.netazoic.ent;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class ENT implements IF_Ent{
	
	public Field nitIDField;
	public String fld_nitID = null;
	public String sql_RetrieveENT;
	public String sql_CreateEVT;
	@JsonIgnore 
	public Connection con;


	public void retrieveRecord() {
		Statement stat = null;
		try{
			Object nitIDObj;
			if(nitIDField == null) 	nitIDField = this.getClass().getField(fld_nitID);
			nitIDObj = nitIDField.get(this);
			if(nitIDObj == null) throw new Exception("Must first set record ID value.");
			String ctp = sql_RetrieveENT;
			Map<String, Object> settings = new HashMap<String, Object>();
			settings.put(nitIDField.getName(), nitIDObj);
			String sql = SQLUtil.parseQuery(settings, ctp);
			stat = con.createStatement();
			ResultSet rs = SQLUtil.execSQL(sql,stat);
			nitID = nitIDObj.toString();
			setFieldVals(rs);
			//twiddleWebuserIterator();
		}catch(Exception ex){
			throw new ClarescoException(ex);
		}finally{
			if(stat != null) try{stat.close();stat = null;}catch(Exception ex){}
		}	

	}


}
