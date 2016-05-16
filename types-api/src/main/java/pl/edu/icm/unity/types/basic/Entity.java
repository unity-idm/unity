/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import com.fasterxml.jackson.annotation.JsonIgnore;

import pl.edu.icm.unity.types.EntityInformation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.CredentialInfo;

/**
 * Represents an entity.
 * @author K. Benedyczak
 */
public class Entity
{
	private Long id;
	private EntityInformation entityInformation;
	private Identity[] identities;
	private CredentialInfo credentialInfo;
	
	public Entity(Long id, Identity[] identities, EntityInformation info, CredentialInfo credentialInfo)
	{
		this.id = id;
		this.identities = identities;
		this.credentialInfo = credentialInfo;
		this.entityInformation = info;
	}

	public Entity(Long id, Identity[] identities, EntityState state, CredentialInfo credentialInfo)
	{
		this(id, identities, new EntityInformation(state), credentialInfo);
	}

	/**
	 * Used by Jackson during deserialization
	 */
	@SuppressWarnings("unused")
	private Entity()
	{
	}
	
	public Long getId()
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

	@JsonIgnore
	public EntityState getState()
	{
		return entityInformation.getState();
	}

	public EntityInformation getEntityInformation()
	{
		return entityInformation;
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
		return String.valueOf(id);
	}
}
