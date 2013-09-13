/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import java.io.IOException;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.mapper.AttributesMapper;
import pl.edu.icm.unity.db.model.AttributeTypeBean;
import pl.edu.icm.unity.db.resolvers.AttributesResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Handles import/export of attribute types table.
 * @author K. Benedyczak
 */
@Component
public class AttributeTypesIE extends AbstractIE
{
	private final AttributesResolver attrResolver;
	private final DBAttributes dbAttributes;
	
	
	@Autowired
	public AttributeTypesIE(ObjectMapper jsonMapper, AttributesResolver attrResolver,
			DBAttributes dbAttributes)
	{
		super(jsonMapper);
		this.attrResolver = attrResolver;
		this.dbAttributes = dbAttributes;
	}


	public void serialize(SqlSession sql, JsonGenerator jg) throws JsonGenerationException, IOException
	{
		AttributesMapper mapper = sql.getMapper(AttributesMapper.class);
		List<AttributeTypeBean> ats = mapper.getAttributeTypes();
		jg.writeStartArray();
		for (AttributeTypeBean bean: ats)
		{
			jg.writeStartObject();
			serializeToJson(jg, bean);
			jg.writeEndObject();
		}
		jg.writeEndArray();
	}
	
	public void deserialize(SqlSession sql, JsonParser input) throws IOException, EngineException
	{
		JsonUtils.expect(input, JsonToken.START_ARRAY);
		while(input.nextToken() == JsonToken.START_OBJECT)
		{
			AttributeTypeBean ab = new AttributeTypeBean();
			deserializeFromJson(input, ab);
			JsonUtils.nextExpect(input, JsonToken.END_OBJECT);
			
			AttributeType toAdd = attrResolver.resolveAttributeTypeBean(ab);
			dbAttributes.addAttributeType(toAdd, sql);
		}
		JsonUtils.expect(input, JsonToken.END_ARRAY);
	}
	
	private void serializeToJson(JsonGenerator jg, AttributeTypeBean at) throws JsonGenerationException, IOException
	{
		super.serializeBaseBeanToJson(jg, at);
		jg.writeStringField("valueSyntaxId", at.getValueSyntaxId());
	}
	
	private void deserializeFromJson(JsonParser input, AttributeTypeBean at) throws IOException
	{
		super.deserializeBaseBeanFromJson(input, at);
		JsonUtils.nextExpect(input, "valueSyntaxId");
		at.setValueSyntaxId(input.getText());
	}
}
