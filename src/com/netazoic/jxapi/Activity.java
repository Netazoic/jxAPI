package com.netazoic.jxapi;

import java.net.URI;
import java.net.URL;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.netazoic.jxapi.iface.IF_Object;
import com.rusticisoftware.tincan.Activity;
import com.rusticisoftware.tincan.ActivityDefinition;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Activity extends com.rusticisoftware.tincan.Activity implements IF_Object {

Long activityID;
URL actvIRI;
String actvName;
String actvDesc;
String activityTypeCode;
String interactionType;

}
