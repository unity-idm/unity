/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Opaque identifier identity. It is useful for storing a generic identifier, being a string. The only requirement is
 * that the string must be non-empty.
 * @author K. Benedyczak
 */
@Component
public class IdentifierIdentity extends AbstractStaticIdentityTypeProvider
{
	public static final String ID = "identifier";
	
	@Override
	public String getId()
	{
		return ID;
	}

	@Override
	public String getDefaultDescriptionKey()
	{
		return "IdentifierIdentity.description";
	}

	@Override
	public void validate(String value)
	{
		if (value == null || value.trim().length() == 0)
		{
			throw new IllegalArgumentException("Identifier must be non empty");
		}
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
	public boolean isDynamic()
	{
		return false;
	}

	@Override
	public String getHumanFriendlyDescription(MessageSource msg)
	{
		return msg.getMessage("IdentifierIdentity.description");
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return msg.getMessage("IdentifierIdentity.name");
	}
}