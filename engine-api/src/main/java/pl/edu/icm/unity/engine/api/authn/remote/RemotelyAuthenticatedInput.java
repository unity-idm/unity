/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.engine.api.session.SessionParticipant;

/**
 * Holds a raw information obtained from an upstream IdP. The purpose of this class is to provide a common interchange
 * format between a pluggable upstream IdP implementation and a fixed code of RemoteVerficator. 
 * <p>
 * The data in this class typically should not be translated, unless an upstream IdP strictly requires some translation
 * to be able to populate the contents. The actual mapping of this data to the locally meaningful information
 * is done using this class as an input.  
 * 
 * @author K. Benedyczak
 */
public class RemotelyAuthenticatedInput
{
	private String idpName;
	private Set<SessionParticipant> sessionParticipants = new HashSet<>();
	private Map<String, RemoteGroupMembership> groups;
	private Map<String, RemoteAttribute> attributes;
	private Map<String, ? extends Object> rawAttributes;
	private Map<String, RemoteIdentity> identities;
	

	public RemotelyAuthenticatedInput(String idpName)
	{
		this.idpName = idpName;
		groups = new HashMap<>();
		attributes = new HashMap<>();
		rawAttributes = new HashMap<>();
		identities = new LinkedHashMap<>();
	}

	public String getIdpName()
	{
		return idpName;
	}
	public void setIdpName(String idpName)
	{
		this.idpName = idpName;
	}

	public void setGroups(List<RemoteGroupMembership> groups)
	{
		for (RemoteGroupMembership gm: groups)
			this.groups.put(gm.getName(), gm);
	}
	public void setAttributes(List<RemoteAttribute> attributes)
	{
		for (RemoteAttribute gm: attributes)
			this.attributes.put(gm.getName(), gm);
	}
	public void setIdentities(List<RemoteIdentity> identities)
	{
		for (RemoteIdentity gm: identities)
			this.identities.put(gm.getName(), gm);
	}
	public void addIdentity(RemoteIdentity gm)
	{
		this.identities.put(gm.getName(), gm);
	}
	public void addAttribute(RemoteAttribute attribute)
	{
		this.attributes.put(attribute.getName(), attribute);
	}
	
	public void addGroup(RemoteGroupMembership group)
	{
		this.groups.put(group.getName(), group);
	}
	
	public Map<String, RemoteGroupMembership> getGroups()
	{
		return groups;
	}

	public Map<String, RemoteAttribute> getAttributes()
	{
		return attributes;
	}

	public Map<String, RemoteIdentity> getIdentities()
	{
		return identities;
	}
	
	public Set<SessionParticipant> getSessionParticipants()
	{
		return sessionParticipants;
	}

	public void addSessionParticipant(SessionParticipant sessionParticipant)
	{
		this.sessionParticipants.add(sessionParticipant);
	}

	public Map<String, ? extends Object> getRawAttributes()
	{
		return rawAttributes;
	}

	public void setRawAttributes(Map<String, ? extends Object> rawAttributes)
	{
		this.rawAttributes = rawAttributes;
	}
	
	@Override
	public String toString()
	{
		return idpName;
	}
	
	/**
	 * @return Multiline string with a complete contents 
	 */
	public String getTextDump()
	{
		StringBuilder sb = new StringBuilder();
		if (!identities.isEmpty())
		{
			sb.append("Identities:\n");
			for (RemoteIdentity id: identities.values())
				sb.append(" - ").append(id).append("\n");
		}
		if (!attributes.isEmpty())
		{
			sb.append("Attributes:\n");
			for (RemoteAttribute at: attributes.values())
				sb.append(" - ").append(at).append("\n");
		}
		if (!groups.isEmpty())
		{
			sb.append("Groups:\n");
			for (RemoteGroupMembership gr: groups.values())
				sb.append(" - ").append(gr).append("\n");
		}
		return sb.toString();
	}
}
