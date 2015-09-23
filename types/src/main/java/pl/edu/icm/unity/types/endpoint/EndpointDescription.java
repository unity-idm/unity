/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.endpoint;

import java.util.List;

import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;

/**
 * Defines a common data required to be provided by each endpoint.
 * 
 * @author K. Benedyczak
 */
public class EndpointDescription
{
	private String id;
	private I18nString displayedName;
	private String contextAddress;
	private String description;
	private AuthenticationRealm realm;
	private EndpointTypeDescription type;
	private List<AuthenticationOptionDescription> authenticatorSets;

	public EndpointDescription(EndpointDescription endpointDesc) 
	{
		id = endpointDesc.getId();
		contextAddress = endpointDesc.getContextAddress(); 
		description = endpointDesc.getDescription();
		realm = endpointDesc.getRealm();
		type = endpointDesc.getType();
		displayedName = endpointDesc.getDisplayedName().clone();
	}
	
	public EndpointDescription(String id, I18nString displayedName, String contextAddress,
			String description, AuthenticationRealm realm,
			EndpointTypeDescription type,
			List<AuthenticationOptionDescription> authenticatorSets)
	{
		this.id = id;
		this.displayedName = displayedName;
		this.contextAddress = contextAddress;
		this.description = description;
		this.realm = realm;
		this.type = type;
		this.authenticatorSets = authenticatorSets;
	}



	public EndpointDescription() 
	{
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
	public List<AuthenticationOptionDescription> getAuthenticatorSets()
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
	public void setAuthenticatorSets(List<AuthenticationOptionDescription> authenticatorSets)
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
	public AuthenticationRealm getRealm()
	{
		return realm;
	}
	public void setRealm(AuthenticationRealm realm)
	{
		this.realm = realm;
	}

	public I18nString getDisplayedName()
	{
		return displayedName;
	}

	public void setDisplayedName(I18nString displayedName)
	{
		this.displayedName = displayedName;
	}
}
