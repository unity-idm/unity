/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import pl.edu.icm.unity.types.basic.Identity;

/**
 * Base class for static identity types, which simply store the identity value in the database.
 * @author K. Benedyczak
 */
public abstract class AbstractStaticIdentityTypeProvider extends AbstractIdentityTypeProvider
{
	@Override
	public Identity createNewIdentity(String realm, String target, long entityId) 
	{
		throw new IllegalStateException("This identity type doesn't support dynamic identity creation.");
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
	public boolean isExpired(Identity idRepresentation)
	{
		return false;
	}
	
	@Override
	public boolean isEmailVerifiable()
	{
		return false;
	}
}
