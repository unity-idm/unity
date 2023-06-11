/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;

/**
 * Serializes {@link AttributeType} to/from {@link AttributeTypeBean}.
 * @author K. Benedyczak
 */
@Component
public class AttributeTypeRDBMSSerializer implements RDBMSObjectSerializer<AttributeType, AttributeTypeBean>
{
	@Autowired
	private ObjectMapper jsonMapper;
	
	@Override
	public AttributeType fromDB(AttributeTypeBean raw)
	{
		DBAttributeTypeBase dbAttribute;
		try
		{
			dbAttribute = jsonMapper.readValue(raw.getContents(), DBAttributeTypeBase.class);
		} catch (IOException e)
		{
			throw new IllegalStateException("Error parsing attribute type from DB", e);
		}
		return AttributeTypeBaseMapper.map(dbAttribute, raw.getName(), raw.getValueSyntaxId());
	}

	@Override
	public AttributeTypeBean toDB(AttributeType at)
	{
		try
		{
			return new AttributeTypeBean(at.getName(), jsonMapper.writeValueAsBytes(AttributeTypeBaseMapper.map(at)),
					at.getValueSyntax());
		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Error saving attribute type to DB", e);

		}
	}
}
