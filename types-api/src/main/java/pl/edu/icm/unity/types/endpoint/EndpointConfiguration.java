/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.endpoint;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base endpoint configuration. Useful when deploying a new endpoint and when updating it. 
 * @author Krzysztof Benedyczak
 */
public class EndpointConfiguration
{
	private I18nString displayedName;
	private String description;
	private List<AuthenticationOptionDescription> authenticationOptions;
	private String configuration;
	private String realm;
	
	public EndpointConfiguration(I18nString displayedName, String description,
			List<AuthenticationOptionDescription> authn, String configuration,
			String realm)
	{
		super();
		this.displayedName = displayedName;
		this.description = description;
		this.authenticationOptions = authn;
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
			ArrayNode aopts = (ArrayNode) json.get("authenticationOptions");
			authenticationOptions = new ArrayList<>();
			for (JsonNode node: aopts)
				authenticationOptions.add(new AuthenticationOptionDescription((ObjectNode) node));
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
		for (AuthenticationOptionDescription aod: authenticationOptions)
			aopts.add(aod.toJson());
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

	public List<AuthenticationOptionDescription> getAuthenticationOptions()
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
				+ description + ", authenticationOptions=" + authenticationOptions
				+ ", configuration=" + configuration + ", realm=" + realm + "]";
	}
}
