/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import pl.edu.icm.unity.base.authn.CredentialInfo;

/**
 * Represents an entity with complete information - as retrieved from the system.
 * @author K. Benedyczak
 */
public class Entity
{
	private EntityInformation entityInformation;
	private List<Identity> identities;
	private CredentialInfo credentialInfo;
	
	public Entity(List<Identity> identities, EntityInformation info, CredentialInfo credentialInfo)
	{
		this.identities = identities;
		this.credentialInfo = credentialInfo;
		this.entityInformation = info;
	}

	/**
	 * Used by Jackson during deserialization
	 */
	@SuppressWarnings("unused")
	private Entity()
	{
	}
	
	public List<Identity> getIdentities()
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

	@JsonIgnore
	public Long getId()
	{
		return entityInformation.getId();
	}

	public EntityInformation getEntityInformation()
	{
		return entityInformation;
	}

	@Override
	public String toString()
	{
		return "Entity [entityInformation=" + entityInformation + ", identities="
				+ identities + ", credentialInfo=" + credentialInfo
				+ "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((credentialInfo == null) ? 0 : credentialInfo.hashCode());
		result = prime * result
				+ ((entityInformation == null) ? 0 : entityInformation.hashCode());
		result = prime * result + ((identities == null) ? 0 : identities.hashCode());
		return result;
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
		if (credentialInfo == null)
		{
			if (other.credentialInfo != null)
				return false;
		} else if (!credentialInfo.equals(other.credentialInfo))
			return false;
		if (entityInformation == null)
		{
			if (other.entityInformation != null)
				return false;
		} else if (!entityInformation.equals(other.entityInformation))
			return false;
		if (identities == null)
		{
			if (other.identities != null)
				return false;
		} else if (!identities.equals(other.identities))
			return false;
		return true;
	}
}
