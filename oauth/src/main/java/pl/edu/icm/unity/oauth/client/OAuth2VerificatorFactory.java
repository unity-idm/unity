/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.net.URL;

import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultProcessor;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Factory of {@link OAuth2Verificator}s.
 * It also installs the {@link ResponseConsumerServlet}.
 * @author K. Benedyczak
 */
@Component
public class OAuth2VerificatorFactory implements CredentialVerificatorFactory
{
	public static final String NAME = "oauth2";
	
	private PKIManagement pkiManagement;
	private URL baseAddress;
	private String baseContext;
	private OAuthContextsManagement contextManagement;
	private RemoteAuthnResultProcessor processor;
	
	@Autowired
	public OAuth2VerificatorFactory(NetworkServer jettyServer,
			SharedEndpointManagement sharedEndpointManagement,
			OAuthContextsManagement contextManagement, 
			PKIManagement pkiManagement,
			RemoteAuthnResultProcessor processor) throws EngineException
	{
		this.processor = processor;
		this.baseAddress = jettyServer.getAdvertisedAddress();
		this.baseContext = sharedEndpointManagement.getBaseContextPath();
		this.contextManagement = contextManagement;
		this.pkiManagement = pkiManagement;
		
		ServletHolder servlet = new ServletHolder(new ResponseConsumerServlet(contextManagement));
		sharedEndpointManagement.deployInternalEndpointServlet(ResponseConsumerServlet.PATH, servlet, false);
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Handles OAuth2 tokens obtained from remote OAuth providers. "
				+ "Queries about additional user information.";
	}

	@Override
	public CredentialVerificator newInstance()
	{
		return new OAuth2Verificator(NAME, getDescription(), contextManagement, 
				pkiManagement, baseAddress, baseContext, processor);
	}

}
