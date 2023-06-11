/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.authn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.describedObject.NamedObject;
import pl.edu.icm.unity.base.i18n.I18nDescribedObject;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.i18n.I18nStringJsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;


/**
 * Configured instance of {@link CredentialType}.
 * <p>
 * Note: configuration is stored as a plain String. This is not JsonObject as while credentials 
 * typically use JSON for configuration, verificators use often properties format, and local verificator configuration
 * is the same as its local credential's one. Therefore to have uniform handling we use universal string
 * also here. 
 * @author K. Benedyczak
 */
public class CredentialDefinition extends I18nDescribedObject implements NamedObject
{
	private String name;
	private String typeId;
	private String configuration;
	private boolean readOnly = false;

	public CredentialDefinition()
	{
		super();
	}

	/**
	 * For tests: the displayed name is set to name (via default value) and description is set empty.
	 * @param typeId
	 * @param name
	 */
	public CredentialDefinition(String typeId, String name)
	{
		this(typeId, name, new I18nString(name), new I18nString(""));
	}
	
	/**
	 * Creates a standard CredentialDefinition with displayed name loaded from message bundle
	 * (using the typeId as key).
	 * @param typeId
	 * @param name
	 * @param msg
	 */
	public CredentialDefinition(String typeId, String name, I18nString description, MessageSource msg)
	{
		this(typeId, name, loadNames(typeId, msg), description);
	}
	
	public CredentialDefinition(String typeId, String name, I18nString displayedName, I18nString description)
	{
		super(displayedName, description);
		this.typeId = typeId;
		this.name = name;
	}
	
	@JsonCreator
	public CredentialDefinition(ObjectNode root)
	{
		fromJson(root);
	}
	
	private static I18nString loadNames(String name, MessageSource msg)
	{
		return new I18nString("CredDef." + name + ".displayedName", msg);
	}
	
	public String getTypeId()
	{
		return typeId;
	}
	public void setTypeId(String typeId)
	{
		this.typeId = typeId;
	}
	public String getConfiguration()
	{
		return configuration;
	}
	public void setConfiguration(String configuration)
	{
		this.configuration = configuration;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
	
	public boolean isReadOnly()
	{
		return readOnly;
	}

	public void setReadOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("typeId", getTypeId());
		root.put("name", getName());
		root.put("readOnly", isReadOnly());
		root.put("configuration", getConfiguration());
		root.set("displayedName", I18nStringJsonUtil.toJson(getDisplayedName()));
		root.set("i18nDescription", I18nStringJsonUtil.toJson(getDescription()));
		return root;
	}
	
	private void fromJson(ObjectNode root)
	{
		JsonNode n;
		n = root.get("name");
		setName(n.asText());

		n = root.get("typeId");
		setTypeId(n.asText());
		
		n = root.get("readOnly");
		if (n != null && !n.isNull())
			setReadOnly(n.asBoolean());
		
		n = root.get("configuration");
		if (n != null && !n.isNull())
			setConfiguration(n.asText());

		if (root.has("displayedName"))
			setDisplayedName(I18nStringJsonUtil.fromJson(root.get("displayedName")));
		else
			setDisplayedName(new I18nString(getName()));

		setDescription(I18nStringJsonUtil.fromJson(root.get("i18nDescription"), 
				root.get("description")));
	}
	
	@Override
	public String toString()
	{
		return "CredentialDefinition [name=" + name + ", typeId=" + typeId + "]";
	}

	@Override
	public CredentialDefinition clone()
	{
		CredentialDefinition ret = new CredentialDefinition(typeId, name, 
				displayedName.clone(), description.clone());
		ret.setConfiguration(configuration);
		ret.setReadOnly(isReadOnly());
		return ret;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((configuration == null) ? 0 : configuration.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((typeId == null) ? 0 : typeId.hashCode());
		result = prime * result + ((readOnly) ? 1 : 0);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CredentialDefinition other = (CredentialDefinition) obj;
		if (configuration == null)
		{
			if (other.configuration != null)
				return false;
		} else if (!configuration.equals(other.configuration))
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (typeId == null)
		{
			if (other.typeId != null)
				return false;
		} else if (!typeId.equals(other.typeId))
			return false;
		if (readOnly != other.readOnly)
			return false;
		
		return true;
	}
}
