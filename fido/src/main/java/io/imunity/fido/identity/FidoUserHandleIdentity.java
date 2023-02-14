/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.identity;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.identity.AbstractStaticIdentityTypeProvider;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * User handle generated automatically for FIDO registration and authentication processes.
 *
 * @author R. Ledzinski
 */
@Component
public class FidoUserHandleIdentity extends AbstractStaticIdentityTypeProvider
{
	public static final String ID = "fidoUserHandle";

	@Override
	public String getId()
	{
		return ID;
	}

	@Override
	public String getDefaultDescriptionKey()
	{
		return "Fido.UserHandleIdentity.description";
	}

	@Override
	public void validate(String value)
	{
	}

	@Override
	public boolean isUserSettable()
	{
		return false;
	}

	@Override
	public Identity createNewIdentity(String realm, String target, long entityId)
	{
		throw new IllegalStateException("This identity type doesn't support dynamic identity creation.");
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
		return msg.getMessage("Fido.UserHandleIdentity.description");
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return msg.getMessage("Fido.UserHandleIdentity.name");
	}
}
