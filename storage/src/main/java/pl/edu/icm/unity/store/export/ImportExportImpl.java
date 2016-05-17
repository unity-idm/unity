/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.impl.attribute.AttributeIE;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypesIE;
import pl.edu.icm.unity.store.impl.entities.EntityIE;
import pl.edu.icm.unity.store.impl.groups.GroupIE;
import pl.edu.icm.unity.store.impl.identities.IdentityIE;
import pl.edu.icm.unity.store.impl.identitytype.IdentityTypeIE;

/**
 * Import/export functionality. 
 * @author K. Benedyczak
 */
@Component
public class ImportExportImpl implements ImportExport
{
	public static final int VERSION = 3;

	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private DumpUpdater updater;
	
	@Autowired
	private AttributeTypesIE attributeTypesIE;

	@Autowired
	private IdentityTypeIE identityTypesIE;

	@Autowired
	private EntityIE entitiesIE;
	
	@Autowired
	private IdentityIE identitiesIE;
	
	@Autowired
	private GroupIE groupsIE;
	
	@Autowired
	private AttributeIE attributesIE;
	
	@Override
	public void store(OutputStream os) throws IOException
	{
		JsonFactory jsonF = new JsonFactory(objectMapper);
		JsonGenerator jg = jsonF.createGenerator(os, JsonEncoding.UTF8);
		jg.useDefaultPrettyPrinter();
		
		jg.writeStartObject();
		
		jg.writeNumberField("versionMajor", VERSION);
		jg.writeNumberField("versionMinor", 0);
		jg.writeNumberField("timestamp", System.currentTimeMillis());

		jg.writeObjectFieldStart("contents");

		jg.writeFieldName("attributeTypes");
		attributeTypesIE.serialize(jg);
		jg.flush();

		jg.writeFieldName("identityTypes");
		identityTypesIE.serialize(jg);
		jg.flush();

		jg.writeFieldName("entities");
		entitiesIE.serialize(jg);
		jg.flush();

		jg.writeFieldName("identities");
		identitiesIE.serialize(jg);
		jg.flush();

		jg.writeFieldName("groups");
		groupsIE.serialize(jg);
		jg.flush();

//		jg.writeFieldName("groupMembers");
//		groupMembersIE.serialize(sql, jg);
//		jg.flush();

		jg.writeFieldName("attributes");
		attributesIE.serialize(jg);
		jg.flush();
//
//		jg.writeFieldName("genericObjects");
//		genericsIE.serialize(sql, jg);
//		jg.flush();
		
		jg.writeEndObject(); //contents
		jg.writeEndObject(); //root
		jg.close();
	}

	@Override
	public void load(InputStream is) throws IOException
	{
		if (!is.markSupported())
			throw new IOException("Only InputStreams supporting mark()&reset() are allowed to load from.");
		
		JsonFactory jsonF = new JsonFactory(objectMapper);
		JsonParser jp = jsonF.createParser(is);
		JsonUtils.nextExpect(jp, JsonToken.START_OBJECT);
		
		DumpHeader header = loadHeader(jp);

		is.mark(-1);
		updater.update(is, header);
		is.reset();
		
		JsonUtils.nextExpect(jp, "contents");
		
		JsonUtils.nextExpect(jp, "attributeTypes");
		attributeTypesIE.deserialize(jp);
		
		JsonUtils.nextExpect(jp, "identityTypes");
		identityTypesIE.deserialize(jp);

		JsonUtils.nextExpect(jp, "entities");
		entitiesIE.deserialize(jp);

		JsonUtils.nextExpect(jp, "identities");
		identitiesIE.deserialize(jp);

		JsonUtils.nextExpect(jp, "groups");
		groupsIE.deserialize(jp);
		
//		JsonUtils.nextExpect(jp, "groupMembers");
//		groupMembersIE.deserialize(sql, jp);

		JsonUtils.nextExpect(jp, "attributes");
		attributesIE.deserialize(jp);

//		JsonUtils.nextExpect(jp, "genericObjects");
//		genericsIE.deserialize(sql, jp);
	}
	
	private DumpHeader loadHeader(JsonParser jp) throws JsonParseException, IOException
	{
		DumpHeader ret = new DumpHeader();
		JsonUtils.nextExpect(jp, "versionMajor");
		ret.setVersionMajor(jp.getIntValue());
		JsonUtils.nextExpect(jp, "versionMinor");
		ret.setVersionMinor(jp.getIntValue());
		JsonUtils.nextExpect(jp, "timestamp");
		ret.setTimestamp(jp.getLongValue());
		return ret;
	}
}
