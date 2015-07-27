/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.server.api.internal.LoginSession;

/**
 * Stores information about authenticated entity during the authentication.
 * <p>
 * This information is stored in {@link LoginSession} along with additional data after authentication is successful.  
 * @author K. Benedyczak
 */
public class AuthenticatedEntity
{
	private Long entityId;
	private boolean usedOutdatedCredential;
	private List<String> authenticatedWith;
	private String remoteIdP;

	public AuthenticatedEntity(Long entityId, String info, boolean useOutdatedCredential)
	{
		this.entityId = entityId;
		this.authenticatedWith = new ArrayList<String>(4);
		authenticatedWith.add(info);
		this.usedOutdatedCredential = useOutdatedCredential;
	}

	/**
	 * @return null in case entity was authenticated locally, id of the remote IdP otherwise.
	 */
	public String getRemoteIdP()
	{
		return remoteIdP;
	}

	public void setRemoteIdP(String remoteIdP)
	{
		this.remoteIdP = remoteIdP;
	}



	public Long getEntityId()
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
