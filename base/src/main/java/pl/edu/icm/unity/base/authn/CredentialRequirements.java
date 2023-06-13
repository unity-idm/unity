/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.authn;

import java.util.Set;

import pl.edu.icm.unity.base.describedObject.DescribedObjectImpl;


/**
 * Set of credentials. It is applied to entities, to define what credentials must be defined/updated.
 * <p>
 * This class uses default JSON serialization
 * @author K. Benedyczak
 */
public class CredentialRequirements extends DescribedObjectImpl
{
	private Set<String> requiredCredentials;
	private boolean readOnly = false;

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

	public boolean isReadOnly()
	{
		return readOnly;
	}

	public void setReadOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
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
		result = prime * result + ((readOnly) ? 1 : 0);
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
		if (readOnly != other.readOnly)
			return false;
		return true;
	}
}
