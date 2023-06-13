/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.AbstractAttributeTypeProvider;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

/**
 * Defines string attribute type used to store user's user's attribute classes.
 * 
 * @author K. Benedyczak
 */
@Component
public class AttributeClassTypeProvider extends AbstractAttributeTypeProvider
{
	public static final String ATTRIBUTE_CLASSES_ATTRIBUTE = "sys:AttributeClasses"; 
	
	@Autowired
	public AttributeClassTypeProvider(MessageSource msg)
	{
		super(msg);
	}
	
	@Override
	protected AttributeType getAttributeType()
	{
		AttributeType preferenceAt = new AttributeType(ATTRIBUTE_CLASSES_ATTRIBUTE, 
				StringAttributeSyntax.ID, msg);
		preferenceAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		preferenceAt.setMinElements(0);
		preferenceAt.setMaxElements(AttributeClassHelper.MAX_CLASSES_PER_ENTITY);
		preferenceAt.setUniqueValues(true);
		return preferenceAt;
	}
}
