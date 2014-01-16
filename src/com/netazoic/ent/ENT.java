package com.netazoic.ent;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netazoic.util.SQLUtil;



public class ENT implements IF_Ent{
	
	public Field nitIDField;
	public String fld_nitID = null;
	public String sql_RetrieveENT;
	public String sql_CreateEVT;
	
	public String nitName = null;
	public String nitID = null;
	public String nitCode = null;
	public String nitTitle = null;
	public String nitDesc = null;
	public String nitURL = null;
	public String nitTypeCode = null;

	public String nitTable = null;

	@JsonIgnore 
	public Connection con;

	public static List<Field> getAllFields(List<Field> fields, Class<?> type) {
		// http://stackoverflow.com/questions/1042798/retrieving-the-inherited-attribute-names-values-using-java-reflection
		for (Field field: type.getDeclaredFields()) {
			fields.add(field);
		}

		if (type.getSuperclass() != null) {
			fields = getAllFields(fields, type.getSuperclass());
		}

		return fields;
	}

	public static List<Field> getAllFields(List<Field> fields, Class<?> type,boolean flgInherit) {
		// http://stackoverflow.com/questions/1042798/retrieving-the-inherited-attribute-names-values-using-java-reflection
		for (Field field: type.getDeclaredFields()) {
			fields.add(field);
		}
		if(flgInherit){
			if (type.getSuperclass() != null) {
				fields = getAllFields(fields, type.getSuperclass());
			}		
		}

		return fields;
	}


	public void retrieveRecord() throws Exception {
		Statement stat = null;
		try{
			Object nitIDObj;
			if(nitIDField == null) 	nitIDField = this.getClass().getField(fld_nitID);
			nitIDObj = nitIDField.get(this);
			if(nitIDObj == null) throw new Exception("Must first set record ID value.");
			String fPath = sql_RetrieveENT;
			Map<String, Object> settings = new HashMap<String, Object>();
			settings.put(nitIDField.getName(), nitIDObj);
			String sql = SQLUtil.parseQueryFile(settings, fPath);
			stat = con.createStatement();
			ResultSet rs = SQLUtil.execSQL(sql,stat);
			nitID = nitIDObj.toString();
			setFieldVals(rs);
			//twiddleWebuserIterator();
		}catch(Exception ex){
			throw ex;
		}finally{
			if(stat != null) try{stat.close();stat = null;}catch(Exception ex){}
		}	

	}

	
	public void setFieldVals(ResultSet rs) throws SQLException{
		//load object fields from similarly named db fields
		/*
		 * At this level, the setFieldVals function can only set values on Public fields in the 
		 * extending class. If you wish to work with private or package scope fields, override
		 * this function with a copy in the local class.
		 */
		List<Field> flds= getAllFields(new LinkedList<Field>(),this.getClass());
		//Field[] flds = this.getClass().getDeclaredFields();
		Object val;
		String fld;
		Class type;
		rs.next();
		int idx = 0;
		ResultSetMetaData rsmd = rs.getMetaData();
		Map<String,Integer> colMap = new HashMap<String,Integer>();
		for (int i = 1; i < rsmd.getColumnCount()+1; i++){
			colMap.put(rsmd.getColumnName(i).toLowerCase(), i);
		}
		for(Field f : flds){
			try{
				fld = f.getName();
				if(!colMap.containsKey(fld.toLowerCase()))continue;
				val = rs.getObject(fld);
				//if(val==null) continue;
				type = f.getType();
				if( type.isInstance(val)){
					//nada, type is the same between rs and object. No conversion necessary.
				}
				else if((type.equals(Integer.class) || (type.equals(int.class))) && ( val instanceof java.math.BigDecimal)){
					BigDecimal mybd = (BigDecimal)val;
					val = mybd.intValueExact();					
				}
				else if(type.equals(String.class) && val instanceof java.math.BigDecimal){
					//need to convert to a String
					val = val.toString();
				}
				else if(type.equals(java.sql.Date.class) && val instanceof java.sql.Timestamp){
					Timestamp ts = (Timestamp)val;
					val = new java.sql.Date(ts.getTime());
				}
				else if(type.equals(java.util.Date.class) && val instanceof java.sql.Timestamp){
					Timestamp ts = (Timestamp)val;
					val = new java.util.Date(ts.getTime());
				}
				else if (type.equals(String.class) && val instanceof java.sql.Date){
					//need to convert Date to a String
					val = val.toString();
				}
				f.set(this, val);
			}catch(Exception ex){
				@SuppressWarnings("unused")
				String msg = ex.getMessage();
				continue;
			}
		}
	}


}
