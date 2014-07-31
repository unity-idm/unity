/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.GenericEntityHandler;
import pl.edu.icm.unity.db.generic.GenericObjectHandlersRegistry;
import pl.edu.icm.unity.db.mapper.GenericMapper;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles import/export of the generic objects table.
 * @author K. Benedyczak
 */
@Component
public class GenericsIE extends AbstractIE
{
	private final DBGeneric dbGeneric;
	private final GenericObjectHandlersRegistry handlersRegistry;

	@Autowired
	public GenericsIE(ObjectMapper jsonMapper, DBGeneric dbGeneric,
			GenericObjectHandlersRegistry handlersRegistry)
	{
		super(jsonMapper);
		this.dbGeneric = dbGeneric;
		this.handlersRegistry = handlersRegistry;
	}

	public void serialize(SqlSession sql, JsonGenerator jg) throws JsonGenerationException, 
			IOException, IllegalTypeException
	{
		GenericMapper mapper = sql.getMapper(GenericMapper.class);
		List<String> types = mapper.selectObjectTypes();
		
		jg.writeStartArray();
		for (String type: types)
		{
			List<GenericObjectBean> generics = dbGeneric.getObjectsOfType(type, sql);
			
			for (GenericObjectBean generic: generics)
			{
				jg.writeStartObject();
				jg.writeStringField("type", generic.getType());
				jg.writeStringField("subType", generic.getSubType());
				jg.writeStringField("name", generic.getName());
				jg.writeNumberField("lastUpdate", generic.getLastUpdate().getTime());
				
				jg.writeFieldName("contents");
				byte[] contents = generic.getContents();
				if (contents != null)
				{
					JsonNode node = jsonMapper.readTree(contents);
					jsonMapper.writeTree(jg, node);
				} else
				{
					jg.writeNull();
				}
				jg.writeEndObject();
			}
		}
		jg.writeEndArray();
	}
	
	public void deserialize(SqlSession sql, JsonParser input) throws IOException, EngineException
	{
		JsonUtils.expect(input, JsonToken.START_ARRAY);

		while (input.nextToken() == JsonToken.START_OBJECT)
		{
			JsonUtils.nextExpect(input, "type");
			String type = input.getText();
			JsonUtils.nextExpect(input, "subType");
			String subType = null;
			if (input.getCurrentToken() != JsonToken.VALUE_NULL)
				subType = input.getText();
			JsonUtils.nextExpect(input, "name");
			String name = input.getText();
			JsonUtils.nextExpect(input, "lastUpdate");
			Date lastUpdate = new Date(input.getLongValue());
			JsonUtils.nextExpect(input, "contents");
			
			JsonNode parsed = null;
			if (input.getCurrentToken() == JsonToken.VALUE_NULL)
			{
				input.nextToken();
			} else
			{
				parsed = jsonMapper.readTree(input);
			}
			JsonUtils.nextExpect(input, JsonToken.END_OBJECT);
			
			GenericEntityHandler<?> handler = handlersRegistry.getByName(type);
			if (handler == null)
				throw new IOException("The generic object type " + type + " is not supported");
			byte[] contents = handler.updateBeforeImport(name, parsed);
			dbGeneric.addObject(name, type, subType, contents, lastUpdate, sql);
		}
		JsonUtils.expect(input, JsonToken.END_ARRAY);
	}
}
