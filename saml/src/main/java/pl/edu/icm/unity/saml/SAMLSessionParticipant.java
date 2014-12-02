/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.server.api.internal.SessionParticipant;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;

/**
 * SAML session participant. Defines type (SAML) and stores entity id and all logout urls together with bindings. 
 * @author K. Benedyczak
 */
public class SAMLSessionParticipant implements SessionParticipant
{
	public static final String TYPE = "SAML2";

	private String identifier;
	private Map<Binding, SAMLEndpointDefinition> logoutEndpoints = new HashMap<Binding, SAMLEndpointDefinition>();
	private String principalNameAtParticipant;
	private String sessionIndex;
	private String localSamlId;
	private String localCredentialName;
	private Set<String> participantsCertificates;
	
	public SAMLSessionParticipant()
	{
		super();
	}

	public SAMLSessionParticipant(String identifier, NameIDType subjectAtParticipant, String sessionIndex,
			List<SAMLEndpointDefinition> logoutEndpoints, String localSamlEntityId,
			String localCredentialName, Set<String> participantsCertificates)
	{
		super();
		this.identifier = identifier;
		for (SAMLEndpointDefinition logout: logoutEndpoints)
			this.logoutEndpoints.put(logout.getBinding(), logout);
		this.principalNameAtParticipant = subjectAtParticipant.xmlText();
		this.sessionIndex = sessionIndex;
		this.localSamlId = localSamlEntityId;
		this.localCredentialName = localCredentialName;
		this.participantsCertificates = new HashSet<>(participantsCertificates);
	}

	@Override
	@JsonIgnore
	public String getProtocolType()
	{
		return TYPE;
	}

	@Override
	public String getIdentifier()
	{
		return identifier;
	}

	public void setIdentifier(String entityId)
	{
		this.identifier = entityId;
	}

	public Map<Binding, SAMLEndpointDefinition> getLogoutEndpoints()
	{
		return new HashMap<Binding, SAMLEndpointDefinition>(logoutEndpoints);
	}

	public void setLogoutEndpoints(Map<Binding, SAMLEndpointDefinition> logoutEndpoints)
	{
		this.logoutEndpoints.putAll(logoutEndpoints);
	}
	
	public String getPrincipalNameAtParticipant()
	{
		return principalNameAtParticipant;
	}

	public String getSessionIndex()
	{
		return sessionIndex;
	}

	public void setPrincipalNameAtParticipant(String principalNameAtParticipant)
	{
		this.principalNameAtParticipant = principalNameAtParticipant;
	}

	public void setSessionIndex(String sessionIndex)
	{
		this.sessionIndex = sessionIndex;
	}

	public String getLocalSamlId()
	{
		return localSamlId;
	}

	public String getLocalCredentialName()
	{
		return localCredentialName;
	}

	public Set<String> getParticipantsCertificates()
	{
		return participantsCertificates;
	}

	@Override
	public String toString()
	{
		return identifier + " SLO: " + logoutEndpoints;
	}
}
