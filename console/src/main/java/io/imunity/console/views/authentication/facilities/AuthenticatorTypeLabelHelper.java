/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorTypeDescription;

public class AuthenticatorTypeLabelHelper
{
	public static String getAuthenticatorTypeLabel(MessageSource msg, AuthenticatorTypeDescription t)
	{
		try
		{
			return msg.getMessageUnsafe("Verificator." + t.getVerificationMethod());
		} catch (Exception e)
		{
			return t.getVerificationMethod() + " (" + t.getVerificationMethodDescription() + ")";
		}
	}
}
