/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Serializes {@link AttributeType} to/from {@link AttributeTypeBean}.
 * @author K. Benedyczak
 */
@Component
public class AttributeTypeRDBMSSerializer implements RDBMSObjectSerializer<AttributeType, AttributeTypeBean>
{
	@Override
	public AttributeType fromDB(AttributeTypeBean raw)
	{
		AttributeType at = new AttributeType();
		at.setName(raw.getName());
		at.setValueSyntax(raw.getValueSyntaxId());
		at.fromJsonBase(JsonUtil.parse(raw.getContents()));
		return at;
	}

	@Override
	public AttributeTypeBean toDB(AttributeType at) 
	{
		return new AttributeTypeBean(at.getName(), JsonUtil.serialize2Bytes(at.toJsonBase()), 
				at.getValueSyntax());
	}
}
