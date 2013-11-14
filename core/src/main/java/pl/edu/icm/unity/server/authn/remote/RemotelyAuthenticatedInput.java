/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a raw information obtained from an upstream IdP. The purpose of this class is to provide a common interchange
 * format between a pluggable upstream IdP implementation and a fixed code of RemoteVerficator. 
 * <p>
 * The data in this class typically should not be translated, unless an upstream IdP strictly requires some translation
 * to be able to populate the contents. The actual mapping of this data to the locally meaningful information
 * is done using this class as input.  
 * 
 * @author K. Benedyczak
 */
public class RemotelyAuthenticatedInput
{
	private String idpName;
	private List<RemoteGroupMembership> groups;
	private List<RemoteAttribute> attributes;
	private List<RemoteIdentity> identities;

	public RemotelyAuthenticatedInput(String idpName)
	{
		this.idpName = idpName;
		groups = new ArrayList<>();
		attributes = new ArrayList<>();
		identities = new ArrayList<>();
	}

	public String getIdpName()
	{
		return idpName;
	}
	public void setIdpName(String idpName)
	{
		this.idpName = idpName;
	}
	public List<RemoteGroupMembership> getGroups()
	{
		return groups;
	}
	public void setGroups(List<RemoteGroupMembership> groups)
	{
		this.groups = groups;
	}
	public List<RemoteAttribute> getAttributes()
	{
		return attributes;
	}
	public void setAttributes(List<RemoteAttribute> attributes)
	{
		this.attributes = attributes;
	}
	public List<RemoteIdentity> getIdentities()
	{
		return identities;
	}
	public void setIdentities(List<RemoteIdentity> identities)
	{
		this.identities = identities;
	}
}
