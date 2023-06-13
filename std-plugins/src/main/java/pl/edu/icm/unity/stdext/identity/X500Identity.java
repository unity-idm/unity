/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * X.500 identity type definition
 * @author K. Benedyczak
 */
@Component
public class X500Identity extends AbstractStaticIdentityTypeProvider
{
	public static final String ID = "x500Name";

	@Override
	public String getId()
	{
		return ID;
	}

	@Override
	public String getDefaultDescriptionKey()
	{
		return "X500Identity.description";
	}

	@Override
	public void validate(String value)
	{
		try
		{
			X500NameUtils.getX500Principal(value);
		} catch (Exception e)
		{
			throw new IllegalArgumentException("DN is invalid: " + 
					e.getMessage(), e);
		}
	}

	@Override
	public String getComparableValue(String from, String realm, String target)
	{
		return X500NameUtils.getComparableForm(from);
	}

	@Override
	public String toPrettyStringNoPrefix(IdentityParam from)
	{
		return X500NameUtils.getReadableForm(from.getValue());
	}


	@Override
	public String getHumanFriendlyDescription(MessageSource msg)
	{
		return msg.getMessage("X500Identity.description");
	}
	
	@Override
	public boolean isDynamic()
	{
		return false;
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return msg.getMessage("X500Identity.name");
	}
}
