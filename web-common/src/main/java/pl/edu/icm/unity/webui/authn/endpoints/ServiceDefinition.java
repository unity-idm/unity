/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.endpoints;

import java.util.List;

import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

/**
 * 
 * @author P.Piernik
 *
 */
public class ServiceDefinition
{
	private String typeId;
	private String name;
	private String address;

	private I18nString displayedName;
	private String description;
	private List<String> authenticationOptions;
	private String configuration;
	private String realm;

	public ServiceDefinition()
	{
		
	}
	
	public ServiceDefinition(ResolvedEndpoint base)
	{
		setTypeId(base.getType().getName());
		setConfiguration(base.getEndpoint().getConfiguration().getConfiguration());
		setAddress(base.getEndpoint().getContextAddress());
		setDescription(base.getEndpoint().getConfiguration().getDescription());
		setDisplayedName(base.getEndpoint().getConfiguration().getDisplayedName());
		setRealm(base.getRealm().getName());
		setAuthenticationOptions(base.getEndpoint().getConfiguration().getAuthenticationOptions());
		setName(base.getName());
	}

	public ServiceDefinition(String type)
	{
		typeId = type;
	}

	public String getTypeId()
	{
		return typeId;
	}

	public void setTypeId(String typeId)
	{
		this.typeId = typeId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getAddress()
	{
		return address;
	}

	public void setAddress(String address)
	{
		this.address = address;
	}

	public I18nString getDisplayedName()
	{
		return displayedName;
	}

	public void setDisplayedName(I18nString displayedName)
	{
		this.displayedName = displayedName;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public List<String> getAuthenticationOptions()
	{
		return authenticationOptions;
	}

	public void setAuthenticationOptions(List<String> authenticationOptions)
	{
		this.authenticationOptions = authenticationOptions;
	}

	public String getConfiguration()
	{
		return configuration;
	}

	public void setConfiguration(String configuration)
	{
		this.configuration = configuration;
	}

	public String getRealm()
	{
		return realm;
	}

	public void setRealm(String realm)
	{
		this.realm = realm;
	}

}
