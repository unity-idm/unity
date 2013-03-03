/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.endpoint;

import java.util.List;

import pl.edu.icm.unity.types.authn.AuthenticatorSet;

/**
 * Defines a common data required to be provided by each endpoint.
 * 
 * @author K. Benedyczak
 */
public class EndpointDescription
{
	private String id;
	private String contextAddress;
	private String description;
	private EndpointTypeDescription type;
	private List<AuthenticatorSet> authenticatorSets;

	public String getId()
	{
		return id;
	}
	public String getDescription()
	{
		return description;
	}
	public EndpointTypeDescription getType()
	{
		return type;
	}
	public List<AuthenticatorSet> getAuthenticatorSets()
	{
		return authenticatorSets;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	public void setType(EndpointTypeDescription type)
	{
		this.type = type;
	}
	public void setAuthenticatorSets(List<AuthenticatorSet> authenticatorSets)
	{
		this.authenticatorSets = authenticatorSets;
	}
	public String getContextAddress()
	{
		return contextAddress;
	}
	public void setContextAddress(String contextAddress)
	{
		this.contextAddress = contextAddress;
	}
}
