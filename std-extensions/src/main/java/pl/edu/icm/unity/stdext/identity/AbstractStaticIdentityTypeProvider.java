/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.IdentityRepresentation;

/**
 * Base class for static identity types, which simply store the identity value in the database.
 * @author K. Benedyczak
 */
public abstract class AbstractStaticIdentityTypeProvider extends AbstractIdentityTypeProvider
{
	@Override
	public String toExternalForm(String realm, String target, String inDbValue, String comparableValue)
	{
		return inDbValue;
	}
	
	@Override
	public String toExternalFormNoContext(String inDbValue, String comparableValue)
	{
		return inDbValue;
	}
	
	@Override
	public IdentityRepresentation createNewIdentity(String realm, String target, String value) 
			throws IllegalTypeException
	{
		throw new IllegalTypeException("This identity type doesn't support dynamic identity creation.");
	}

	@Override
	public boolean isDynamic()
	{
		return false;
	}
	
	@Override
	public boolean isTargeted()
	{
		return false;
	}
	
	@Override
	public boolean isExpired(IdentityRepresentation idRepresentation)
	{
		return false;
	}
	
	@Override
	public boolean isVerifiable()
	{
		return false;
	}
}
