/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.endpoint;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;

/**
 * Base endpoint configuration. Useful when deploying a new endpoint and when updating it. 
 * @author Krzysztof Benedyczak
 */
public class EndpointConfiguration
{
	private I18nString displayedName;
	private String description;
	private List<String> authenticationOptions;
	private String configuration;
	private String realm;
	
	public EndpointConfiguration(I18nString displayedName, String description,
			List<String> authnOptions, String configuration,
			String realm)
	{
		super();
		this.displayedName = displayedName;
		this.description = description;
		this.authenticationOptions = authnOptions;
		this.configuration = configuration;
		this.realm = realm;
	}

	@JsonCreator
	public EndpointConfiguration(ObjectNode json)
	{
		if (json.has("displayedName"))
			displayedName = I18nStringJsonUtil.fromJson(json.get("displayedName"));
		if (json.has("description"))
			description = json.get("description").asText();
		if (json.has("configuration"))
			configuration = json.get("configuration").asText();
		if (json.has("realm"))
			realm = json.get("realm").asText();
		if (json.has("authenticationOptions"))
		{
			authenticationOptions = new ArrayList<>();
			ArrayNode aopts = (ArrayNode) json.get("authenticationOptions");
			for (JsonNode node: aopts)
				authenticationOptions.add(node.asText());
			
		}
	}
	
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.set("displayedName", I18nStringJsonUtil.toJson(displayedName));
		root.put("description", description);
		root.put("configuration", configuration);
		root.put("realm", realm);
		ArrayNode aopts = root.withArray("authenticationOptions");
		for (String aod : authenticationOptions)
			aopts.add(aod);
		return root;
	}
	
	public I18nString getDisplayedName()
	{
		return displayedName;
	}

	public String getDescription()
	{
		return description;
	}

	public List<String> getAuthenticationOptions()
	{
		return authenticationOptions;
	}

	public String getConfiguration()
	{
		return configuration;
	}

	public String getRealm()
	{
		return realm;
	}

	@Override
	public String toString()
	{
		return "EndpointConfiguration [displayedName=" + displayedName + ", description="
				+ description + ", authnOptions=" + authenticationOptions
				+ ", configuration=" + configuration + ", realm=" + realm + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((authenticationOptions == null) ? 0
				: authenticationOptions.hashCode());
		result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((displayedName == null) ? 0 : displayedName.hashCode());
		result = prime * result + ((realm == null) ? 0 : realm.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EndpointConfiguration other = (EndpointConfiguration) obj;
		if (authenticationOptions == null)
		{
			if (other.authenticationOptions != null)
				return false;
		} else if (!authenticationOptions.equals(other.authenticationOptions))
			return false;
		if (configuration == null)
		{
			if (other.configuration != null)
				return false;
		} else if (!configuration.equals(other.configuration))
			return false;
		if (description == null)
		{
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (displayedName == null)
		{
			if (other.displayedName != null)
				return false;
		} else if (!displayedName.equals(other.displayedName))
			return false;
		if (realm == null)
		{
			if (other.realm != null)
				return false;
		} else if (!realm.equals(other.realm))
			return false;
		return true;
	}
}
