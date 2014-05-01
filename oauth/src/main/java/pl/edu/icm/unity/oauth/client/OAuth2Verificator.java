/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest.AuthenticationRequestBuilder;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.ResponseType;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;


/**
 * Binding independent OAuth 2 logic. Creates authZ requests, validates response (OAuth authorization grant)
 * performs subsequent call to AS to get resource owner's (authenticated user) information.
 *   
 * @author K. Benedyczak
 */
public class OAuth2Verificator extends AbstractRemoteVerificator implements OAuthExchange
{
	private OAuthClientProperties config;
	private String responseConsumerAddress;
	private OAuthContextsManagement contextManagement;
	
	public OAuth2Verificator(String name, String description, OAuthContextsManagement contextManagement,
			TranslationProfileManagement profileManagement, AttributesManagement attrMan,
			URL baseAddress, String baseContext)
	{
		super(name, description, OAuthExchange.ID, profileManagement, attrMan);
		this.responseConsumerAddress = baseAddress + baseContext + ResponseConsumerServlet.PATH;
		this.contextManagement = contextManagement;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		StringWriter sbw = new StringWriter();
		try
		{
			config.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize OAuth2 verificator configuration", e);
		}
		return sbw.toString();	
	}

	@Override
	public void setSerializedConfiguration(String source) throws InternalException
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			config = new OAuthClientProperties(properties);
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the OAuth2 verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the OAuth2 verificator(?)", e);
		}
	}

	@Override
	public OAuthClientProperties getSettings()
	{
		return config;
	}

	@Override
	public OAuthClientRequest createRequest() throws OAuthSystemException
	{
		String clientId = config.getValue(OAuthClientProperties.CLIENT_ID);
		String authzEndpoint = config.getValue(OAuthClientProperties.PROVIDER_LOCATION);
		String scopes = config.getValue(OAuthClientProperties.SCOPES);
		OAuthContext context = new OAuthContext();
		AuthenticationRequestBuilder requestBuilder = OAuthClientRequest.authorizationLocation(authzEndpoint)
				.setClientId(clientId)
				.setScope(scopes)
				.setRedirectURI(responseConsumerAddress)
				.setResponseType(ResponseType.CODE.toString())
				.setState(context.getRelayState());
		List<String> params = config.getListOfValues(OAuthClientProperties.PARAMS);
		for (String param: params)
		{
			String[] sParam = param.split("=");
			requestBuilder.setParameter(sParam[0], sParam[1]);
		}
		OAuthClientRequest request = requestBuilder.buildQueryMessage();
		contextManagement.addAuthnContext(context);
		return request;
	}

	@Override
	public AuthenticationResult verifyOAuthAuthzResponse(OAuthAuthzResponse oar)
	{
		
		// TODO Auto-generated method stub
		return null;
	}
}
