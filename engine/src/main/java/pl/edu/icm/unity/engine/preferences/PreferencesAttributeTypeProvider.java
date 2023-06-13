/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.preferences;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.AbstractAttributeTypeProvider;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

/**
 * Defines string attribute type used to store user's preferences.
 * @author K. Benedyczak
 */
@Component
public class PreferencesAttributeTypeProvider extends AbstractAttributeTypeProvider
{
	public static final String PREFERENCES = "sys:Preferences";
	
	@Autowired
	public PreferencesAttributeTypeProvider(MessageSource msg)
	{
		super(msg);
	}
	
	@Override
	protected AttributeType getAttributeType()
	{
		AttributeType preferenceAt = new AttributeType(PREFERENCES, StringAttributeSyntax.ID, msg);
		preferenceAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		preferenceAt.setMinElements(1);
		preferenceAt.setMaxElements(1);
		preferenceAt.setUniqueValues(false);
		return preferenceAt;
	}
}
