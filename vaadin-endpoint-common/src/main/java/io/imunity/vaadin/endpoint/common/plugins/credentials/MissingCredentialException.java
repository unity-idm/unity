/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials;

import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;

public class MissingCredentialException extends IllegalCredentialException
{
	public MissingCredentialException(String msg)
	{
		super(msg);
	}
}
