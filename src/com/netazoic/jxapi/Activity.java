package com.netazoic.jxapi;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.databind.JsonNode;
import com.netazoic.jxapi.iface.IF_Object;
import com.rusticisoftware.tincan.ActivityDefinition;

public class Activity extends com.rusticisoftware.tincan.Activity implements IF_Object {

public Activity(JsonNode js) throws URISyntaxException {
		super(js);
	}
public Activity(String string) throws URISyntaxException {
	super(string);
}
String activityID;
URL actvIRI;
String actvName;
String actvDesc;
String activityTypeCode;
String interactionType;

}
