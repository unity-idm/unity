/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.authenticators;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;

/**
 * 
 * @author P.Piernik
 *
 */
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
