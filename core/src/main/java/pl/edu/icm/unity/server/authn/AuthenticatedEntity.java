/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

/**
 * Stores information about authenticated entity.
 * @author K. Benedyczak
 */
public class AuthenticatedEntity
{
	private long entityId;

	public AuthenticatedEntity(long entityId)
	{
		this.entityId = entityId;
	}

	public long getEntityId()
	{
		return entityId;
	}

	public void setEntityId(long entityId)
	{
		this.entityId = entityId;
	}
}
