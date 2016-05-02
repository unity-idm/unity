/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.attributes.AttributeTypeSerializer;
import pl.edu.icm.unity.base.attributes.AttributeValueSyntaxFactory;
import pl.edu.icm.unity.base.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;

/**
 * Serializes {@link AttributeType} to/from {@link AttributeTypeBean}.
 * @author K. Benedyczak
 */
@Component
public class AttributeTypeRDBMSSerializer implements RDBMSObjectSerializer<AttributeType, AttributeTypeBean>
{
	@Autowired
	private AttributeSyntaxFactoriesRegistry typesRegistry;
	@Autowired
	private AttributeTypeSerializer atSerializer;
	
	@Override
	public AttributeType fromDB(AttributeTypeBean raw)
	{
		AttributeType at = new AttributeType();
		at.setName(raw.getName());
		AttributeValueSyntaxFactory<?> syntaxFactory = typesRegistry.getByName(raw.getValueSyntaxId());
		@SuppressWarnings("rawtypes")
		AttributeValueSyntax syntax = syntaxFactory.createInstance();
		at.setValueType(syntax);
		atSerializer.fromJson(JsonUtil.parse(raw.getContents()), at);
		return at;
	}

	@Override
	public AttributeTypeBean toDB(AttributeType at) 
	{
		return new AttributeTypeBean(at.getName(), JsonUtil.serialize2Bytes(atSerializer.toJsonNode(at)), 
				at.getValueType().getValueSyntaxId());
	}
}
