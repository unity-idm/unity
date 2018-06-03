/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

/**
 * Represents a remote identity
 * @author K. Benedyczak
 */
public class RemoteIdentity extends RemoteInformationBase
{
	private String identityType;
	
	public RemoteIdentity(String name, String type)
	{
		super(name);
		this.identityType = type;
	}

	public String getIdentityType()
	{
		return identityType;
	}

	public void setIdentityType(String identityType)
	{
		this.identityType = identityType;
	}
	
	@Override
	public String toString()
	{
		return getName() + " (" + identityType + ")";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((identityType == null) ? 0 : identityType.hashCode());
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
		RemoteIdentity other = (RemoteIdentity) obj;
		if (identityType == null)
		{
			if (other.identityType != null)
				return false;
		} else if (!identityType.equals(other.identityType))
			return false;
		return true;
	}
}
