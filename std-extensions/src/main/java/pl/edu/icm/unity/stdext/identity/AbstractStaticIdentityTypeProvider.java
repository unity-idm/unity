/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import pl.edu.icm.unity.exceptions.IllegalTypeException;

/**
 * Base class for static identity types, which simply store the identity value in the database.
 * @author K. Benedyczak
 */
public abstract class AbstractStaticIdentityTypeProvider extends AbstractIdentityTypeProvider
{
	@Override
	public String toExternalForm(String realm, String target, String inDbValue)
	{
		return inDbValue;
	}

	@Override
	public String createNewIdentity(String realm, String target, String inDbValue)
			throws IllegalTypeException
	{
		throw new IllegalTypeException("This identity type doesn't support dynamic identity creation.");
	}
	

	@Override
	public String resetIdentity(String realm, String target, String inDbValue)
			throws IllegalTypeException
	{
		throw new IllegalTypeException("This identity type doesn't support dynamic identity reset.");
	}
}
