/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;

public class MissingCredentialException extends IllegalCredentialException
{
	public MissingCredentialException(String msg)
	{
		super(msg);
	}
}
