/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Implementation of {@link AttributeTypeSupport}, most often proxing to {@link AttributeTypeHelper} with transactions
 * added on top of it.
 * 
 * @author K. Benedyczak
 */

@Component
public class AttributeTypeSupportImpl implements AttributeTypeSupport
{
	@Autowired
	private AttributeTypeHelper aTypeHelper;
	
	@Transactional
	@Override
	public AttributeValueSyntax<?> getSyntax(AttributeType at)
	{
		return aTypeHelper.getSyntax(at);
	}

	@Transactional
	@Override
	public AttributeValueSyntax<?> getSyntax(Attribute attribute)
	{
		return aTypeHelper.getSyntaxForAttributeName(attribute.getName());
	}

	@Transactional
	@Override
	public AttributeType getType(Attribute attribute)
	{
		return aTypeHelper.getTypeForAttributeName(attribute.getName());
	}

	@Transactional
	@Override
	public Collection<AttributeType> getAttributeTypes()
	{
		return aTypeHelper.getAttributeTypes();
	}

	@Transactional
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

	@Transactional
	@Override
	public AttributeType getType(String attribute)
	{
		return aTypeHelper.getTypeForAttributeName(attribute);
	}


	@Override
	public List<AttributeType> loadAttributeTypesFromResource(Resource r)
	{
		return aTypeHelper.loadAttributeTypesFromResource(r);
	}

	@Override
	public List<Resource> getAttibuteTypeResourcesFromClasspathDir()
	{
		return aTypeHelper.getAttibuteTypeResourcesFromClasspathDir();
	}
}
