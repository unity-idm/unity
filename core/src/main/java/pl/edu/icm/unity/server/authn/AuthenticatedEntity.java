/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.types.authn.LocalAuthenticationState;

/**
 * Stores information about authenticated entity.
 * @author K. Benedyczak
 */
public class AuthenticatedEntity
{
	private long entityId;
	private LocalAuthenticationState authnState;

	public AuthenticatedEntity(long entityId, LocalAuthenticationState authnState)
	{
		this.entityId = entityId;
		this.authnState = authnState;
	}

	public long getEntityId()
	{
		return entityId;
	}

	public void setEntityId(long entityId)
	{
		this.entityId = entityId;
	}

	public LocalAuthenticationState getAuthnState()
	{
		return authnState;
	}

	public void setAuthnState(LocalAuthenticationState authnState)
	{
		this.authnState = authnState;
	}
}
