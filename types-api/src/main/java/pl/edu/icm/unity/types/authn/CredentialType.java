/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import pl.edu.icm.unity.types.DescribedObjectROImpl;


/**
 * Describes credential type as username and password or one-time password.
 * Each credential can optionally support invalidation. This means that instances can be put into the
 * {@link LocalCredentialState#outdated} state.
 * @author K. Benedyczak
 */
public class CredentialType extends DescribedObjectROImpl
{
	private boolean supportingInvalidation;
	
	public CredentialType()
	{
		super();
	}

	public CredentialType(String name, String description, boolean supportsInvalidation)
	{
		super(name, description);
		this.supportingInvalidation = supportsInvalidation;
	}

	public boolean isSupportingInvalidation()
	{
		return supportingInvalidation;
	}

	public void setSupportingInvalidation(boolean supportingInvalidation)
	{
		this.supportingInvalidation = supportingInvalidation;
	}
}
