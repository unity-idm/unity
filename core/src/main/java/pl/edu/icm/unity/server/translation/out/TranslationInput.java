/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
	private Set<String> groups;
	private String requester;
	private String protocol;
	private String protocolSubType;
	
	public TranslationInput(List<? extends Attribute<?>> attributes, Entity entity,
			Set<String> groups, String requester, String protocol,
			String protocolSubType)
	{
		super();
		this.attributes = Collections.unmodifiableList(attributes);
		this.entity = entity;
		this.groups = Collections.unmodifiableSet(groups);
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
		if (!groups.isEmpty())
		{
			sb.append("Groups: " + groups + "\n");
		}
		sb.append("Requester: " + requester + "\n");
		sb.append("Protocol: " + protocol + ":" + protocolSubType);
		return sb.toString();
	}
}
