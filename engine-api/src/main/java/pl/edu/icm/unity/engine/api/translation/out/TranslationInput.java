/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.out;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;

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
	private Set<Group> groups;
	private String requester;
	private Collection<Attribute> requesterAttributes;
	private String protocol;
	private String protocolSubType;
	private Map<String, Status> importStatus;
	private RemoteAuthnMetadata remoteAuthnMetadata;
	
	public TranslationInput(Collection<? extends Attribute> attributes, Entity entity, String chosenGroup,
			Collection<Group> groups, 
			String requester, Collection<? extends Attribute> requesterAttributes,
			String protocol,
			String protocolSubType, Map<String, Status> importStatus, RemoteAuthnMetadata remoteAuthnMetadata)
	{
		this.requesterAttributes = Lists.newArrayList(requesterAttributes);
		this.importStatus = importStatus;
		this.attributes = Lists.newArrayList(attributes);
		this.entity = entity;
		this.chosenGroup = chosenGroup;
		this.groups = new HashSet<>();
		this.groups.addAll(groups);
		this.requester = requester;
		this.protocol = protocol;
		this.protocolSubType = protocolSubType;
		this.remoteAuthnMetadata = remoteAuthnMetadata;
	}

	public Collection<Attribute> getAttributes()
	{
		return attributes;
	}
	public Entity getEntity()
	{
		return entity;
	}
	public Set<Group> getGroups()
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

	public Collection<Attribute> getRequesterAttributes()
	{
		return requesterAttributes;
	}

	@Override
	public String toString()
	{
		return requester + " - eId: " + entity.getId();
	}

	public RemoteAuthnMetadata getRemoteAuthnMetadata()
	{
		return remoteAuthnMetadata;
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
		if (!requesterAttributes.isEmpty())
		{
			sb.append("Requester attributes:\n");
			for (Attribute at: requesterAttributes)
				sb.append(" - ").append(at).append("\n");
		}
		sb.append("Protocol: " + protocol + ":" + protocolSubType + "\n");
		sb.append("remoteAuthnMetadata: " + remoteAuthnMetadata);

		return sb.toString();
	}
}
