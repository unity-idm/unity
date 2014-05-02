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

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest.AuthenticationRequestBuilder;
import org.apache.oltu.oauth2.client.response.GitHubTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;


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
	public OAuthContext createRequest(String providerKey) throws OAuthSystemException
	{
		String clientId = config.getValue(providerKey + OAuthClientProperties.CLIENT_ID);
		String clientSecret = config.getValue(providerKey + OAuthClientProperties.CLIENT_SECRET);
		String authzEndpoint = config.getValue(providerKey + OAuthClientProperties.PROVIDER_LOCATION);
		String tokenEndpoint = config.getValue(providerKey + OAuthClientProperties.ACCESS_TOKEN_ENDPOINT);
		String profileEndpoint = config.getValue(providerKey + OAuthClientProperties.PROFILE_ENDPOINT);
		String scopes = config.getValue(providerKey + OAuthClientProperties.SCOPES);
		String registrationForm = config.getValue(providerKey + OAuthClientProperties.REGISTRATION_FORM);
		boolean nonJsonMode = config.getBooleanValue(providerKey + OAuthClientProperties.NON_JSON_MODE);
		OAuthContext context = new OAuthContext();
		AuthenticationRequestBuilder requestBuilder = OAuthClientRequest.authorizationLocation(authzEndpoint)
				.setClientId(clientId)
				.setScope(scopes)
				.setRedirectURI(responseConsumerAddress)
				.setResponseType(ResponseType.CODE.toString())
				.setState(context.getRelayState());
		List<String> params = config.getListOfValues(providerKey + OAuthClientProperties.PARAMS);
		for (String param: params)
		{
			String[] sParam = param.split("=");
			requestBuilder.setParameter(sParam[0], sParam[1]);
		}
		OAuthClientRequest request = requestBuilder.buildQueryMessage();
		context.setRequest(request, registrationForm, clientId, clientSecret, tokenEndpoint, 
				profileEndpoint, nonJsonMode);
		contextManagement.addAuthnContext(context);
		return context;
	}

	/**
	 * The real OAuth workhorse. The authz code response verification needs not to be done: the state is 
	 * correct as otherwise there would be no match with the {@link OAuthContext}. However we need to
	 * use the authz code to retrieve access token. The access code may include everything we need. But it 
	 * may also happen that we need to perform one more query to obtain additional profile information.
	 * @throws AuthenticationException 
	 *   
	 */
	@Override
	public AuthenticationResult verifyOAuthAuthzResponse(OAuthContext context) throws AuthenticationException
	{
		OAuthProblemException error = context.getError();
		if (error != null)
		{
			throw new AuthenticationException("OAuth provider returned an error: " + 
					error.getError() + (error.getDescription() != null ? 
							" " + error.getDescription() : ""), error);
		}
		
		OAuthAccessTokenResponse accessToken;
		try
		{
			accessToken = retrieveAccessToken(context);
		} catch (OAuthSystemException e)
		{
			throw new AuthenticationException("Error processing OAuth access token query response", e);
		} catch (OAuthProblemException e)
		{
			throw new AuthenticationException("OAuth provider's access token endpoint returned an error : " + 
					e.getError() + (e.getDescription() != null ? " " + e.getDescription() : ""), e);
		}
		
		if (context.getProfileEndpoint() != null)
			retrieveProfileInformation(context);
		
		RemotelyAuthenticatedInput input = convertInput(context, accessToken);
		
		// TODO Auto-generated method stub
		return null;
	}
	
	private OAuthAccessTokenResponse retrieveAccessToken(OAuthContext context) 
			throws OAuthSystemException, OAuthProblemException
	{
		OAuthClientRequest request = OAuthClientRequest
				.tokenLocation(context.getTokenEndpoint())
				.setGrantType(GrantType.AUTHORIZATION_CODE)
				.setClientId(context.getClientId())
				.setClientSecret(context.getClientSecret())
				.setRedirectURI(responseConsumerAddress)
				.setCode(context.getAuthzResponse().getCode())
				.buildQueryMessage();
	
		OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
		Class<? extends OAuthAccessTokenResponse> responseClass = context.isNonJsonMode() ? 
				GitHubTokenResponse.class : OAuthJSONAccessTokenResponse.class;
		OAuthAccessTokenResponse accessToken = oAuthClient.accessToken(request, responseClass);
		return accessToken;
	}
	
	private void retrieveProfileInformation(OAuthContext context)
	{
		//TODO
	}
	
	private RemotelyAuthenticatedInput convertInput(OAuthContext context, OAuthAccessTokenResponse accessToken)
	{
		/*
		Open
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput(context.getTokenEndpoint());
		accessToken.get
		input.
		*/
		//TODO
		return null;
	}
}







