/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.types.authn.LocalAuthenticationState;

/**
 * Stores information about authenticated entity.
 * @author K. Benedyczak
 */
public class AuthenticatedEntity
{
	private long entityId;
	private List<String> authenticatedWith;
	private LocalAuthenticationState authnState;

	public AuthenticatedEntity(long entityId, LocalAuthenticationState authnState, String info)
	{
		this.entityId = entityId;
		this.authnState = authnState;
		this.authenticatedWith = new ArrayList<String>(4);
		authenticatedWith.add(info);
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

	public List<String> getAuthenticatedWith()
	{
		return authenticatedWith;
	}

	public void setAuthenticatedWith(List<String> authenticatedWith)
	{
		this.authenticatedWith = authenticatedWith;
	}
}
