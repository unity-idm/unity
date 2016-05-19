/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.Set;

import pl.edu.icm.unity.types.DescribedObjectImpl;


/**
 * Set of credentials. It is applied to entities, to define what credentials must be defined/updated.
 * <p>
 * This class uses default JSON serialization
 * @author K. Benedyczak
 */
public class CredentialRequirements extends DescribedObjectImpl
{
	private Set<String> requiredCredentials;

	public CredentialRequirements()
	{
		super();
	}

	public CredentialRequirements(String name, String description,
			Set<String> requiredCredentials)
	{
		super(name, description);
		this.requiredCredentials = requiredCredentials;
	}

	public Set<String> getRequiredCredentials()
	{
		return requiredCredentials;
	}

	public void setRequiredCredentials(Set<String> requiredCredentials)
	{
		this.requiredCredentials = requiredCredentials;
	}

	@Override
	public String toString()
	{
		return "CredentialRequirements [requiredCredentials=" + requiredCredentials + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((requiredCredentials == null) ? 0
				: requiredCredentials.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CredentialRequirements other = (CredentialRequirements) obj;
		if (requiredCredentials == null)
		{
			if (other.requiredCredentials != null)
				return false;
		} else if (!requiredCredentials.equals(other.requiredCredentials))
			return false;
		return true;
	}
}
