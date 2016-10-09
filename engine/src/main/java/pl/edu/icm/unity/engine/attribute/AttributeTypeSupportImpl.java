/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Implementation of {@link AttributeTypeSupport}, proxing to {@link AttributeTypeHelper} with transactions
 * added on top of it.
 * 
 * @author K. Benedyczak
 */
@Component
@Transactional
public class AttributeTypeSupportImpl implements AttributeTypeSupport
{
	@Autowired
	private AttributeTypeHelper aTypeHelper;

	@Override
	public AttributeValueSyntax<?> getSyntax(AttributeType at)
	{
		return aTypeHelper.getSyntax(at);
	}

	@Override
	public AttributeValueSyntax<?> getSyntax(Attribute attribute)
	{
		return aTypeHelper.getSyntaxForAttributeName(attribute.getName());
	}

	@Override
	public AttributeType getType(Attribute attribute)
	{
		return aTypeHelper.getTypeForAttributeName(attribute.getName());
	}

	@Override
	public Collection<AttributeType> getAttributeTypes()
	{
		return aTypeHelper.getAttributeTypes();
	}

	@Override
	public AttributeValueSyntax<?> getSyntaxFallingBackToDefault(Attribute attribute)
	{
		try
		{
			AttributeType aType = getType(attribute);
			return aTypeHelper.getSyntax(aType);
		} catch (Exception e)
		{
			return aTypeHelper.getUnconfiguredSyntax(attribute.getValueSyntax());
		}
	}
}
