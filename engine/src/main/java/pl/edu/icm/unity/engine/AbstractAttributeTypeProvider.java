/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.List;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.SystemAttributesProvider;

/**
 * Abstract base implementation of a {@link SystemAttributesProvider}, useful when a single attribute type 
 * is returned which doesn't require update.  
 *  
 * @author K. Benedyczak
 */
public abstract class AbstractAttributeTypeProvider implements SystemAttributesProvider
{
	private List<AttributeType> systemAttributes;
	protected MessageSource msg;
	
	public AbstractAttributeTypeProvider(MessageSource msg)
	{
		this.msg = msg;
	}
	
	protected abstract AttributeType getAttributeType();

	@Override
	public boolean requiresUpdate(AttributeType at)
	{
		return false;
	}

	@Override
	public List<AttributeType> getSystemAttributes()
	{
		
		return systemAttributes == null ? Lists.newArrayList(getAttributeType()) : systemAttributes;
	}
}
