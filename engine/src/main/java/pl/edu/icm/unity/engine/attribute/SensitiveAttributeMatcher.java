/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider;
import pl.edu.icm.unity.types.basic.AttributeExt;

/**
 * Filters attributes which should not be normally sent outside Unity, due to security concerns.
 * There are certain operations e.g. via REST API where those attributes still can be obtained, 
 * but for regular ones those should be hidden. 
 * 
 * @author K. Benedyczak
 */
public class SensitiveAttributeMatcher
{
	static boolean isSensitive(AttributeExt a)
	{
		//this should be done in a better way: attribute type should advertise this as a flag
		return a.getName().startsWith(CredentialAttributeTypeProvider.CREDENTIAL_PREFIX);
	}

	static boolean isNotSensitive(AttributeExt a)
	{
		return !isSensitive(a);
	}

}
