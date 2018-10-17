/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Throws to indicate that user already exists in the system. Used mainly in
 * registration scenarios to distinguish whether submitted identity exists or
 * not.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class IdentityExistsException extends IllegalFormContentsException
{
	public IdentityExistsException(String msg)
	{
		super(msg);
	}

}
