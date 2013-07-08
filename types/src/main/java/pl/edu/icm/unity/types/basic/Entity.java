/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.CredentialInfo;

/**
 * Represents an entity.
 * @author K. Benedyczak
 */
public class Entity
{
	private String id;
	private EntityState state;
	private Identity[] identities;
	private CredentialInfo credentialInfo;
	
	public Entity(String id, Identity[] identities, EntityState state, CredentialInfo credentialInfo)
	{
		this.id = id;
		this.identities = identities;
		this.credentialInfo = credentialInfo;
		this.state = state;
	}

	public String getId()
	{
		return id;
	}
	public Identity[] getIdentities()
	{
		return identities;
	}

	public CredentialInfo getCredentialInfo()
	{
		return credentialInfo;
	}

	public EntityState getState()
	{
		return state;
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Entity other = (Entity) obj;
		if (id == null)
		{
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return id;
	}
}
