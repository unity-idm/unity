/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.identity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.base.describedObject.NamedObject;
import pl.edu.icm.unity.base.utils.JsonUtil;

/**
 * Type of identity. Contains stateful configuration which can be modified by administrator.
 * 
 * @author K. Benedyczak
 */
public class IdentityType implements NamedObject
{
	private String name;
	private String identityTypeProvider;
	private String identityTypeProviderSettings;
	private String description = "";
	private boolean selfModificable;
	private int minInstances = 0;
	private int maxInstances = Integer.MAX_VALUE;
	private int minVerifiedInstances = 0;
	private EmailConfirmationConfiguration emailConfirmationConfiguration;

	public IdentityType(String name)
	{
		this.name = name;
	}
	
	public IdentityType(String name, String identityTypeProvider)
	{
		this(name);
		this.identityTypeProvider = identityTypeProvider;
	}
	
	public IdentityType(String name, String identityTypeProvider, String description)
	{
		this(name, identityTypeProvider);
		this.description = description;
	}

	@JsonCreator
	public IdentityType(ObjectNode root)
	{
		fromJson(root);
	}
	
	
	public String getIdentityTypeProvider()
	{
		return identityTypeProvider;
	}

	public void setIdentityTypeProvider(String identityTypeProvider)
	{
		this.identityTypeProvider = identityTypeProvider;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public boolean isSelfModificable()
	{
		return selfModificable;
	}

	public void setSelfModificable(boolean selfModificable)
	{
		this.selfModificable = selfModificable;
	}

	public int getMinInstances()
	{
		return minInstances;
	}

	public void setMinInstances(int minInstances)
	{
		this.minInstances = minInstances;
	}

	public int getMaxInstances()
	{
		return maxInstances;
	}

	public void setMaxInstances(int maxInstances)
	{
		this.maxInstances = maxInstances;
	}

	public int getMinVerifiedInstances()
	{
		return minVerifiedInstances;
	}

	public void setMinVerifiedInstances(int minVerifiedInstances)
	{
		this.minVerifiedInstances = minVerifiedInstances;
	}

	public String getIdentityTypeProviderSettings()
	{
		return identityTypeProviderSettings;
	}

	public void setIdentityTypeProviderSettings(String identityTypeProviderSettings)
	{
		this.identityTypeProviderSettings = identityTypeProviderSettings;
	}
	
	public EmailConfirmationConfiguration getEmailConfirmationConfiguration()
	{
		return emailConfirmationConfiguration;
	}

	public void setEmailConfirmationConfiguration(EmailConfirmationConfiguration confirmationConfiguration)
	{
		this.emailConfirmationConfiguration = confirmationConfiguration;
	}

	@Override
	public String getName()
	{
		return name;
	}

	
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode main = toJsonBase();
		main.put("name", getName());
		return main;
	}

	public ObjectNode toJsonBase()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("identityTypeProvider", getIdentityTypeProvider());
		main.put("description", getDescription());
		main.put("selfModificable", isSelfModificable());
		main.put("minInstances", getMinInstances());
		main.put("maxInstances", getMaxInstances());
		main.put("minVerifiedInstances", getMinVerifiedInstances());
		main.put("identityTypeProviderSettings", getIdentityTypeProviderSettings());
		if (getEmailConfirmationConfiguration() != null)
			main.set("emailConfirmationConfiguration", getEmailConfirmationConfiguration().toJson());
		return main;
	}

	private void fromJson(ObjectNode main)
	{
		name = main.get("name").asText();
		fromJsonBase(main);
	}

	public void fromJsonBase(ObjectNode main)
	{
		identityTypeProvider = main.get("identityTypeProvider").asText();
		setDescription(main.get("description").asText());
		setSelfModificable(main.get("selfModificable").asBoolean());
		setMinInstances(main.get("minInstances").asInt());
		setMinVerifiedInstances(main.get("minVerifiedInstances").asInt());
		setMaxInstances(main.get("maxInstances").asInt());
		setIdentityTypeProviderSettings(JsonUtil.getWithDef(main, "identityTypeProviderSettings", null));
		if (main.get("emailConfirmationConfiguration") != null)
			setEmailConfirmationConfiguration(new EmailConfirmationConfiguration(
					(ObjectNode) main.get("emailConfirmationConfiguration")));
	}
	
	public IdentityType clone()
	{
		ObjectNode json = toJson();
		return new IdentityType(json);
	}
	
	public String toString()
	{
		return "[" + getIdentityTypeProvider() + "] " + description;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((identityTypeProvider == null) ? 0
				: identityTypeProvider.hashCode());
		result = prime * result + ((identityTypeProviderSettings == null) ? 0
				: identityTypeProviderSettings.hashCode());
		result = prime * result + maxInstances;
		result = prime * result + minInstances;
		result = prime * result + minVerifiedInstances;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (selfModificable ? 1231 : 1237);
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
		IdentityType other = (IdentityType) obj;
		if (description == null)
		{
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (identityTypeProvider == null)
		{
			if (other.identityTypeProvider != null)
				return false;
		} else if (!identityTypeProvider.equals(other.identityTypeProvider))
			return false;
		if (identityTypeProviderSettings == null)
		{
			if (other.identityTypeProviderSettings != null)
				return false;
		} else if (!identityTypeProviderSettings.equals(other.identityTypeProviderSettings))
			return false;
		if (maxInstances != other.maxInstances)
			return false;
		if (minInstances != other.minInstances)
			return false;
		if (minVerifiedInstances != other.minVerifiedInstances)
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (selfModificable != other.selfModificable)
			return false;
		return true;
	}
}

