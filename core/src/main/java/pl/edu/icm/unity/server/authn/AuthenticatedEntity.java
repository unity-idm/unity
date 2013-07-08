/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores information about authenticated entity.
 * @author K. Benedyczak
 */
public class AuthenticatedEntity
{
	private long entityId;
	private boolean usedOutdatedCredential;
	private List<String> authenticatedWith;

	public AuthenticatedEntity(long entityId, String info, boolean useOutdatedCredential)
	{
		this.entityId = entityId;
		this.authenticatedWith = new ArrayList<String>(4);
		authenticatedWith.add(info);
		this.usedOutdatedCredential = useOutdatedCredential;
	}

	public long getEntityId()
	{
		return entityId;
	}

	public void setEntityId(long entityId)
	{
		this.entityId = entityId;
	}

	public List<String> getAuthenticatedWith()
	{
		return authenticatedWith;
	}

	public void setAuthenticatedWith(List<String> authenticatedWith)
	{
		this.authenticatedWith = authenticatedWith;
	}

	public boolean isUsedOutdatedCredential()
	{
		return usedOutdatedCredential;
	}

	public void setUsedOutdatedCredential(boolean usedOutdatedCredential)
	{
		this.usedOutdatedCredential = usedOutdatedCredential;
	}
}
