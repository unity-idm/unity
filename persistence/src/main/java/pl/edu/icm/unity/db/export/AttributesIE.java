/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.mapper.AttributesMapper;
import pl.edu.icm.unity.db.model.AttributeBean;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.resolvers.AttributesResolver;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.Attribute;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles import/export of attributes table.
 * @author K. Benedyczak
 */
@Component
public class AttributesIE extends AbstractIE
{
	private final GroupResolver groupResolver;
	private final DBAttributes dbAttributes;
	private final AttributesResolver attributesResolver;

	@Autowired
	public AttributesIE(ObjectMapper jsonMapper, GroupResolver groupResolver,
			DBAttributes dbAttributes, AttributesResolver attributesResolver)
	{
		super(jsonMapper);
		this.groupResolver = groupResolver;
		this.dbAttributes = dbAttributes;
		this.attributesResolver = attributesResolver;
	}


	public void serialize(SqlSession sql, JsonGenerator jg) throws JsonGenerationException, 
			IOException, IllegalTypeException
	{
		Map<String, GroupBean> sortedGroups = GroupsIE.getSortedGroups(sql, groupResolver);
		AttributesMapper mapper = sql.getMapper(AttributesMapper.class);
		
		jg.writeStartArray();
		for (Map.Entry<String, GroupBean> entry: sortedGroups.entrySet())
		{
			List<AttributeBean> attrsInGroup = dbAttributes.getDefinedAttributes(
					null, entry.getValue().getId(), null, mapper);

			for (AttributeBean attr: attrsInGroup)
			{
				jg.writeStartObject();
				jg.writeStringField("groupPath", entry.getKey());
				jg.writeNumberField("entity", attr.getEntityId());
				jg.writeStringField("attributeName", attr.getName());
				jg.writeStringField("valueSyntaxId", attr.getValueSyntaxId());
				jg.writeBinaryField("values", attr.getValues());
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
			JsonUtils.nextExpect(input, "groupPath");
			String path = input.getText();
			JsonUtils.nextExpect(input, "entity");
			long entityId = input.getLongValue();
			JsonUtils.nextExpect(input, "attributeName");
			String attributeName = input.getText();
			JsonUtils.nextExpect(input, "valueSyntaxId");
			String valueSyntaxId = input.getText();
			JsonUtils.nextExpect(input, "values");
			byte[] values = input.getBinaryValue();
			JsonUtils.nextExpect(input, JsonToken.END_OBJECT);
	
			AttributeBean bean = new AttributeBean();
			bean.setEntityId(entityId);
			bean.setName(attributeName);
			bean.setValues(values);
			bean.setValueSyntaxId(valueSyntaxId);
			Attribute<?> attr = attributesResolver.resolveAttributeBean(bean, path);
			dbAttributes.addAttribute(entityId, attr, false, sql);
		}
		JsonUtils.expect(input, JsonToken.END_ARRAY);
	}
}
