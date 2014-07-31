/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
	private Collection<Attribute<?>> attributes;
	private Entity entity;
	private String chosenGroup;
	private Set<String> groups;
	private String requester;
	private String protocol;
	private String protocolSubType;
	
	public TranslationInput(Collection<? extends Attribute<?>> attributes, Entity entity, String chosenGroup,
			Collection<String> groups, String requester, String protocol,
			String protocolSubType)
	{
		super();
		this.attributes = new ArrayList<Attribute<?>>();
		this.attributes.addAll(attributes);
		this.entity = entity;
		this.chosenGroup = chosenGroup;
		this.groups = new HashSet<String>();
		this.groups.addAll(groups);
		this.requester = requester;
		this.protocol = protocol;
		this.protocolSubType = protocolSubType;
	}

	public Collection<Attribute<?>> getAttributes()
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

	@Override
	public String toString()
	{
		return requester + " - eId: " + entity;
	}

	/**
	 * @return Multiline string with a complete contents 
	 */
	public String getTextDump()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Entity " + entity.toString() + ":\n");
		for (Identity id: entity.getIdentities())
			sb.append(" - ").append(id.toPrettyString()).append("\n");
		if (!attributes.isEmpty())
		{
			sb.append("Attributes:\n");
			for (Attribute<?> at: attributes)
				sb.append(" - ").append(at).append("\n");
		}
		sb.append("In group: " + chosenGroup + "\n");
		if (!groups.isEmpty())
		{
			sb.append("Groups: " + groups + "\n");
		}
		sb.append("Requester: " + requester + "\n");
		sb.append("Protocol: " + protocol + ":" + protocolSubType);
		return sb.toString();
	}
}
