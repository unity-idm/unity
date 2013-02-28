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
	private String description;
	private EndpointTypeDescription type;
	private List<AuthenticatorSet> authenticatorSets;

	public EndpointDescription(String id, String description, EndpointTypeDescription type,
			List<AuthenticatorSet> authenticatorSets)
	{
		this.id = id;
		this.description = description;
		this.type = type;
		this.authenticatorSets = authenticatorSets;
	}
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
}
