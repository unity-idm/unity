/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * Represents an entity.
 * @author K. Benedyczak
 */
public class Entity
{
	private String id;
	private Identity[] identities;
	
	public Entity(String id, Identity[] identities)
	{
		this.id = id;
		this.identities = identities;
	}

	public String getId()
	{
		return id;
	}
	public Identity[] getIdentities()
	{
		return identities;
	}
}
