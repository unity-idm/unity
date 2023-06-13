/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.endpoint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.i18n.I18nStringJsonUtil;

/**
 * Base endpoint configuration. Useful when deploying a new endpoint and when
 * updating it.
 * 
 * @author Krzysztof Benedyczak
 */
public class EndpointConfiguration
{
	private I18nString displayedName;
	private String description;
	private List<String> authenticationOptions;
	private String configuration;
	private String realm;
	private String tag;

	public EndpointConfiguration(I18nString displayedName, String description, List<String> authnOptions,
			String configuration, String realm)
	{
		this.displayedName = displayedName;
		this.description = description;
		this.authenticationOptions = authnOptions;
		this.configuration = configuration;
		this.realm = realm;
		this.tag = generateTag(configuration);
	}

	public EndpointConfiguration(I18nString displayedName, String description, List<String> authnOptions,
			String configuration, String realm, String tag)
	{
		this(displayedName, description, authnOptions, configuration, realm);
		this.tag = tag;
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
			for (JsonNode node : aopts)
				authenticationOptions.add(node.asText());

		}
		
		tag = json.has("tag") ? json.get("tag").asText() : generateTag(configuration);
	}

	private String generateTag(String configuration)
	{
		if (configuration != null)
		{
			try
			{
				byte[] digest = MessageDigest.getInstance("SHA-256").digest(
						configuration.getBytes(StandardCharsets.UTF_8));
				return Base64.getEncoder().encodeToString(digest);
			} catch (NoSuchAlgorithmException e)
			{
				throw new IllegalStateException("Can not generate message fingerprint "
						+ "with SHA 256, java platform problem?", e);
			}
		} else
		{
			return "";
		}
	}
	
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.set("displayedName", I18nStringJsonUtil.toJson(displayedName));
		root.put("description", description);
		root.put("configuration", configuration);
		if (realm != null)
		{
			root.put("realm", realm);
		}
		if (tag != null)
		{
			root.put("tag", tag);
		}
		ArrayNode aopts = root.withArray("authenticationOptions");
		if (authenticationOptions != null)
		{
			for (String aod : authenticationOptions)
				aopts.add(aod);
		}
			
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

	public String getTag()
	{
		return tag;
	}

	public void setTag(String tag)
	{
		this.tag = tag;
	}

	@Override
	public String toString()
	{
		return "EndpointConfiguration [displayedName=" + displayedName + ", description=" + description
				+ ", authnOptions=" + authenticationOptions + ", configuration=" + configuration
				+ ", realm=" + realm + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((authenticationOptions == null) ? 0 : authenticationOptions.hashCode());
		result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((displayedName == null) ? 0 : displayedName.hashCode());
		result = prime * result + ((realm == null) ? 0 : realm.hashCode());
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
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

		if (tag == null)
		{
			if (other.tag != null)
				return false;
		} else if (!tag.equals(other.tag))
			return false;

		return true;
	}
}
