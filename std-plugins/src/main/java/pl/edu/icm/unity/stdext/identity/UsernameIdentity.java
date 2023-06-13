/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Simple username identity type definition
 * @author K. Benedyczak
 */
@Component
public class UsernameIdentity extends AbstractStaticIdentityTypeProvider
{
	public static final String ID = "userName";
	
	@Override
	public String getId()
	{
		return ID;
	}

	@Override
	public String getDefaultDescriptionKey()
	{
		return "UsernameIdentity.description";
	}

	@Override
	public void validate(String value)
	{
		if (value == null || value.trim().length() == 0)
		{
			throw new IllegalArgumentException("Username must be non empty");
		}
	}
	
	@Override
	public IdentityParam convertFromString(String stringRepresentation, String remoteIdp, 
			String translationProfile) throws IllegalIdentityValueException
	{
		return super.convertFromString(stringRepresentation.trim(), remoteIdp, translationProfile);
	}
	
	@Override
	public String getComparableValue(String from, String realm, String target)
	{
		return from;
	}

	@Override
	public String toPrettyStringNoPrefix(IdentityParam from)
	{
		return from.getValue();
	}


	@Override
	public String getHumanFriendlyDescription(MessageSource msg)
	{
		return msg.getMessage("UsernameIdentity.description");
	}
	
	@Override
	public boolean isDynamic()
	{
		return false;
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return msg.getMessage("UsernameIdentity.name");
	}
}
