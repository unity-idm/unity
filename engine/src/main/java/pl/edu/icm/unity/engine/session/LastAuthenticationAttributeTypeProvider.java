/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.AbstractAttributeTypeProvider;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

/**
 * Defines string attribute type used to store user's last authentication timestamp
 * 
 * @author K. Benedyczak
 */
@Component
public class LastAuthenticationAttributeTypeProvider extends AbstractAttributeTypeProvider
{
	public static final String LAST_AUTHENTICATION = "sys:LastAuthentication";
	
	@Autowired
	public LastAuthenticationAttributeTypeProvider(MessageSource msg)
	{
		super(msg);
	}
	
	@Override
	protected AttributeType getAttributeType()
	{
		AttributeType preferenceAt = new AttributeType(LAST_AUTHENTICATION, StringAttributeSyntax.ID, msg);
		preferenceAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		preferenceAt.setMinElements(0);
		preferenceAt.setMaxElements(1);
		return preferenceAt;
	}
}
