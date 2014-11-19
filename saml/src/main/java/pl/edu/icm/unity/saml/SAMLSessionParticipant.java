/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import java.util.HashMap;
import java.util.Map;

import eu.unicore.samly2.SAMLBindings;
import pl.edu.icm.unity.server.api.internal.SessionParticipant;

/**
 * SAML session participant. Defines type (SAML) and stores entity id and all logout urls together with bindings. 
 * @author K. Benedyczak
 */
public class SAMLSessionParticipant implements SessionParticipant
{
	public static final String TYPE = "SAML2";

	private String identifier;
	private Map<String, String> logoutEndpoints = new HashMap<String, String>();
	
	public SAMLSessionParticipant()
	{
		super();
	}

	public SAMLSessionParticipant(String identifier, Map<String, String> logoutEndpoints)
	{
		super();
		this.identifier = identifier;
		this.logoutEndpoints = logoutEndpoints;
	}

	public SAMLSessionParticipant(String identifier, String soapLogoutEndpoint, String postLogoutEndpoint, 
			String redirectLogoutEndpoint)
	{
		super();
		this.identifier = identifier;
		if (postLogoutEndpoint != null)
			logoutEndpoints.put(SAMLBindings.HTTP_POST.name(), postLogoutEndpoint);
		if (redirectLogoutEndpoint != null)
			logoutEndpoints.put(SAMLBindings.HTTP_REDIRECT.name(), redirectLogoutEndpoint);
		if (soapLogoutEndpoint != null)
			logoutEndpoints.put(SAMLBindings.SOAP.name(), soapLogoutEndpoint);
	}

	@Override
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

	public Map<String, String> getLogoutEndpoints()
	{
		return new HashMap<String, String>(logoutEndpoints);
	}

	public void setLogoutEndpoints(Map<String, String> logoutEndpoints)
	{
		this.logoutEndpoints.putAll(logoutEndpoints);
	}
	
	@Override
	public String toString()
	{
		return identifier + " SLO: " + logoutEndpoints;
	}
}
