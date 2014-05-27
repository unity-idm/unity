/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

import java.util.ArrayList;
import java.util.Collection;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Holds information about a user which was obtained and verified by a remote upstream IdP.
 * The information in this class in in Unity format but need not to have counterparts in the local database.
 * 
 * @author K. Benedyczak
 */
public class RemotelyAuthenticatedContext
{
	private String remoteIdPName;
	private Collection<IdentityTaV> identities = new ArrayList<>();
	private IdentityTaV primaryIdentity;
	private Collection<Attribute<?>> attributes = new ArrayList<>();
	private Collection<String> groups = new ArrayList<>();

	public RemotelyAuthenticatedContext(String remoteIdPName)
	{
		this.remoteIdPName = remoteIdPName;
		try
		{
			InvocationContext ctx = InvocationContext.getCurrent();
			if (ctx.getTlsIdentity() != null)
				identities.add(ctx.getTlsIdentity());
		} catch (InternalException e)
		{
			//OK
		}
	}
	public Collection<IdentityTaV> getIdentities()
	{
		return identities;
	}
	public void addIdentities(Collection<IdentityTaV> identities)
	{
		this.identities.addAll(identities);
	}
	public Collection<Attribute<?>> getAttributes()
	{
		return attributes;
	}
	public void addAttributes(Collection<Attribute<?>> attributes)
	{
		this.attributes.addAll(attributes);
	}
	public Collection<String> getGroups()
	{
		return groups;
	}
	public void addGroups(Collection<String> groups)
	{
		this.groups.addAll(groups);
	}
	public IdentityTaV getPrimaryIdentity()
	{
		return primaryIdentity;
	}
	public void setPrimaryIdentity(IdentityTaV primaryIdentity)
	{
		this.primaryIdentity = primaryIdentity;
	}
	public String getRemoteIdPName()
	{
		return remoteIdPName;
	}
}