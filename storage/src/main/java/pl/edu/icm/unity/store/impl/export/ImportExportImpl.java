/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.export;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import pl.edu.icm.unity.store.api.ImportExport;

/**
 * Import/export functionality. 
 * @author K. Benedyczak
 */
@Component
public class ImportExportImpl implements ImportExport
{
	public static final int VERSION = 3;

	@Autowired
	private AttributeTypesIE attributeTypesIE;
	
	@Override
	public void storeToFile(File file) throws IOException
	{
		JsonFactory jsonF = new JsonFactory();
		JsonGenerator jg = jsonF.createGenerator(file, JsonEncoding.UTF8);
		jg.useDefaultPrettyPrinter();
		
		jg.writeStartObject();
		
		jg.writeNumberField("versionMajor", VERSION);
		jg.writeNumberField("versionMinor", 0);
		jg.writeNumberField("timestamp", System.currentTimeMillis());

		jg.writeObjectFieldStart("contents");

		jg.writeFieldName("attributeTypes");
		attributeTypesIE.serialize(jg);
		jg.flush();

//		jg.writeFieldName("identityTypes");
//		identityTypesIE.serialize(sql, jg);
//		jg.flush();
//
//		jg.writeFieldName("entities");
//		entitiesIE.serialize(sql, jg);
//		jg.flush();
//
//		jg.writeFieldName("identities");
//		identitiesIE.serialize(sql, jg);
//		jg.flush();
//
//		jg.writeFieldName("groups");
//		groupsIE.serialize(sql, jg);
//		jg.flush();
//
//		jg.writeFieldName("groupMembers");
//		groupMembersIE.serialize(sql, jg);
//		jg.flush();
//
//		jg.writeFieldName("attributes");
//		attributesIE.serialize(sql, jg);
//		jg.flush();
//
//		jg.writeFieldName("genericObjects");
//		genericsIE.serialize(sql, jg);
//		jg.flush();
		
		jg.writeEndObject(); //contents
		jg.writeEndObject(); //root
		jg.close();
	}

	@Override
	public void loadFromFile(File file)
	{
		// TODO Auto-generated method stub
		
	}
}
