/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import java.io.IOException;

import pl.edu.icm.unity.db.model.BaseBean;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Few bits of shared code among Import/Export modules
 * @author K. Benedyczak
 */
public abstract class AbstractIE
{
	protected final ObjectMapper jsonMapper;

	public AbstractIE(ObjectMapper jsonMapper)
	{
		this.jsonMapper = jsonMapper;
	}
	
	protected void serializeBaseBeanToJson(JsonGenerator jg, BaseBean at) throws JsonGenerationException, IOException
	{
		jg.writeNumberField("id", at.getId());
		jg.writeStringField("name", at.getName());
		
		
		jg.writeFieldName("contents");
		byte[] contents = at.getContents();
		if (contents != null)
		{
			JsonNode node = jsonMapper.readTree(contents);
			jsonMapper.writeTree(jg, node);
		} else
		{
			jg.writeNull();
		}
	}
	
	protected void deserializeBaseBeanFromJson(JsonParser input, BaseBean at) throws IOException
	{
		JsonUtils.nextExpect(input, "id");
		at.setId(input.getLongValue());
		JsonUtils.nextExpect(input, "name");
		at.setName(input.getText());
		JsonUtils.nextExpect(input, "contents");
		if (input.getCurrentToken() == JsonToken.VALUE_NULL)
		{
			input.nextToken();
			at.setContents(null);
		} else
		{
			JsonNode parsed = jsonMapper.readTree(input);
			at.setContents(jsonMapper.writeValueAsBytes(parsed));
		}
	}
}
