/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.out;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Translation input: a complete information about an entity. Immutable.
 * 
 * @author K. Benedyczak
 */
public class TranslationInput
{
	private Collection<Attribute> attributes;
	private Entity entity;
	private String chosenGroup;
	private Set<String> groups;
	private String requester;
	private String protocol;
	private String protocolSubType;
	private Map<String, Status> importStatus;
	
	public TranslationInput(Collection<? extends Attribute> attributes, Entity entity, String chosenGroup,
			Collection<String> groups, String requester, String protocol,
			String protocolSubType, Map<String, Status> importStatus)
	{
		this.importStatus = importStatus;
		this.attributes = new ArrayList<>();
		this.attributes.addAll(attributes);
		this.entity = entity;
		this.chosenGroup = chosenGroup;
		this.groups = new HashSet<>();
		this.groups.addAll(groups);
		this.requester = requester;
		this.protocol = protocol;
		this.protocolSubType = protocolSubType;
	}

	public Collection<Attribute> getAttributes()
	{
		return attributes;
	}
	public Entity getEntity()
	{
		return entity;
	}
	public Set<String> getGroups()
	{
		return groups;
	}
	public String getRequester()
	{
		return requester;
	}
	public String getProtocol()
	{
		return protocol;
	}
	public String getProtocolSubType()
	{
		return protocolSubType;
	}
	public String getChosenGroup()
	{
		return chosenGroup;
	}

	public Map<String, Status> getImportStatus()
	{
		return importStatus;
	}

	public void setImportStatus(Map<String, Status> importStatus)
	{
		this.importStatus = importStatus;
	}

	@Override
	public String toString()
	{
		return requester + " - eId: " + entity.getId();
	}

	/**
	 * @return Multiline string with a complete contents 
	 */
	public String getTextDump()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Entity " + entity.getId() + ":\n");
		for (Identity id: entity.getIdentities())
			sb.append(" - ").append(id.toString()).append("\n");
		if (!attributes.isEmpty())
		{
			sb.append("Attributes:\n");
			for (Attribute at: attributes)
				sb.append(" - ").append(at).append("\n");
		}
		sb.append("In group: " + chosenGroup + "\n");
		if (!groups.isEmpty())
		{
			sb.append("Groups: " + groups + "\n");
		}
		if (!importStatus.isEmpty())
		{
			sb.append("User import status:\n");
			for (Map.Entry<String, Status> entry: importStatus.entrySet())
				sb.append(" - ").append(entry.getKey()).append(": ")
					.append(entry.getValue()).append("\n");
		}
		sb.append("Requester: " + requester + "\n");
		sb.append("Protocol: " + protocol + ":" + protocolSubType);

		return sb.toString();
	}
}
