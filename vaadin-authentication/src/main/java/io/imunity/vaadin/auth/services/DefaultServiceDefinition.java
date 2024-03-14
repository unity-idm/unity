/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services;

import pl.edu.icm.unity.base.endpoint.Endpoint.EndpointState;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.i18n.I18nString;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains information necessary for create or update service
 * 
 * @author P.Piernik
 *
 */
public class DefaultServiceDefinition implements ServiceDefinition
{
	private String typeId;
	private String name;
	private String address;
	private I18nString displayedName;
	private String description;
	private List<String> authenticationOptions;
	private String configuration;
	private String realm;
	private EndpointState state;
	private String binding;
	private boolean supportsConfigReloadFromFile;

	public DefaultServiceDefinition()
	{
		displayedName = new I18nString();
		authenticationOptions = new ArrayList<>();
	}

	public DefaultServiceDefinition(ResolvedEndpoint base)
	{
		setTypeId(base.getType().getName());
		setConfiguration(base.getEndpoint().getConfiguration().getConfiguration());
		setAddress(base.getEndpoint().getContextAddress());
		setDescription(base.getEndpoint().getConfiguration().getDescription());
		setDisplayedName(base.getEndpoint().getConfiguration().getDisplayedName());
		setRealm(base.getRealm() == null ? null : base.getRealm().getName());
		setAuthenticationOptions(base.getEndpoint().getConfiguration().getAuthenticationOptions());
		setName(base.getName());
		setState(base.getEndpoint().getState());
		setBinding(base.getType().getSupportedBinding());
	}

	public DefaultServiceDefinition(String type)
	{
		this();
		typeId = type;
	}

	public String getType()
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
		return description == null ? "" : description;
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

	@Override
	public EndpointState getState()
	{
		return state;
	}

	@Override
	public String getBinding()
	{
		return binding;
	}

	public void setState(EndpointState state)
	{
		this.state = state;
	}

	public void setBinding(String binding)
	{
		this.binding = binding;
	}
	
	@Override
	public boolean supportsConfigReloadFromFile()
	{
		return supportsConfigReloadFromFile;
	}

	public void setSupportsConfigReloadFromFile(boolean supportsConfigReloadFromFile)
	{
		this.supportsConfigReloadFromFile = supportsConfigReloadFromFile;
	}
}
