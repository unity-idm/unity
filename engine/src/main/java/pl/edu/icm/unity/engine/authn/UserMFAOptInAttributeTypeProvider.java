/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.authn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.AbstractAttributeTypeProvider;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Defines string attribute type used to store user's optin
 * @author P.Piernik
 *
 */
@Component
public class UserMFAOptInAttributeTypeProvider extends AbstractAttributeTypeProvider
{
	public static final String USER_MFA_OPT_IN = "sys:userMFAOptIn";
	
	@Autowired
	public UserMFAOptInAttributeTypeProvider(UnityMessageSource msg)
	{
		super(msg);
	}
	
	@Override
	protected AttributeType getAttributeType()
	{
		AttributeType userOptinAt = new AttributeType(USER_MFA_OPT_IN, StringAttributeSyntax.ID, msg);
		userOptinAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		userOptinAt.setMinElements(1);
		userOptinAt.setMaxElements(1);
		userOptinAt.setUniqueValues(false);
		return userOptinAt;
	}
}

