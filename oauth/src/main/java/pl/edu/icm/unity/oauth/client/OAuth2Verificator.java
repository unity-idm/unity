/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;


/**
 * Binding independent OAuth 2 logic. Creates authZ requests, validates response (OAuth authorization grant)
 * performs subsequent call to AS to get resource owner's (authenticated user) information.
 *   
 * @author K. Benedyczak
 */
public class OAuth2Verificator extends AbstractRemoteVerificator implements OAuthExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuth2Verificator.class);
	public static final String DEFAULT_TOKEN_EXPIRATION = "3600";
	private OAuthClientProperties config;
	private String responseConsumerAddress;
	private OAuthContextsManagement contextManagement;
	private OpenIdProviderMetadataManager metadataManager;
	private PKIManagement pkiManagement;
	
	public OAuth2Verificator(String name, String description, OAuthContextsManagement contextManagement,
			TranslationProfileManagement profileManagement, InputTranslationEngine trEngine,
			PKIManagement pkiManagement, URL baseAddress, String baseContext)
	{
		super(name, description, OAuthExchange.ID, profileManagement, trEngine);
		this.responseConsumerAddress = baseAddress + baseContext + ResponseConsumerServlet.PATH;
		this.contextManagement = contextManagement;
		this.pkiManagement = pkiManagement;
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
			config = new OAuthClientProperties(properties, pkiManagement);
			metadataManager = new OpenIdProviderMetadataManager();
			Set<String> keys = config.getStructuredListKeys(OAuthClientProperties.PROVIDERS);
			for (String key: keys)
			{
				if (config.getProvider(key).getBooleanValue(CustomProviderProperties.OPENID_CONNECT))
				{
					metadataManager.addProvider(config.getProvider(key).getValue(
							CustomProviderProperties.OPENID_DISCOVERY));
				}
			}
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
	public OAuthContext createRequest(String providerKey) throws URISyntaxException, SerializeException, 
		ParseException, IOException
	{
		CustomProviderProperties providerCfg = config.getProvider(providerKey); 
		String clientId = providerCfg.getValue(CustomProviderProperties.CLIENT_ID);
		String authzEndpoint = providerCfg.getValue(CustomProviderProperties.PROVIDER_LOCATION);
		
		String scopes = providerCfg.getValue(CustomProviderProperties.SCOPES);
		boolean openidMode = providerCfg.getBooleanValue(CustomProviderProperties.OPENID_CONNECT);

		OAuthContext context = new OAuthContext();
		AuthorizationRequest req;
		if (openidMode)
		{
			if (authzEndpoint == null)
			{
				String discoveryEndpoint = providerCfg.getValue(CustomProviderProperties.OPENID_DISCOVERY);
				OIDCProviderMetadata providerMeta = metadataManager.getMetadata(discoveryEndpoint, 
						providerCfg);
				if (providerMeta.getAuthorizationEndpointURI() == null)
					throw new ConfigurationException("The authorization endpoint address is not set and"
							+ " it is not available in the discovered OpenID Provider metadata.");
				authzEndpoint = providerMeta.getAuthorizationEndpointURI().toString();
			}
			req = new AuthenticationRequest(
				new URI(authzEndpoint),
				new ResponseType(ResponseType.Value.CODE),
				Scope.parse(scopes),
				new ClientID(clientId),
				new URI(responseConsumerAddress),
				new State(context.getRelayState()),
				null);
		} else
		{
			Scope scope = scopes == null ? null : Scope.parse(scopes);
			req = new AuthorizationRequest(
					new URI(authzEndpoint),
					new ResponseType(ResponseType.Value.CODE),
					null,
					new ClientID(clientId),
					new URI(responseConsumerAddress),
					scope, 
					new State(context.getRelayState()));
		}
		
		context.setRequest(req, req.toURI(), providerKey);
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
		RemoteAuthnState state = startAuthnResponseProcessing(context.getSandboxCallback(), 
				Log.U_SERVER_TRANSLATION, Log.U_SERVER_OAUTH);
		try
		{
			RemotelyAuthenticatedInput input = getRemotelyAuthenticatedInput(context);
			String translationProfile = config.getProvider(context.getProviderConfigKey()).getValue( 
					CommonWebAuthnProperties.TRANSLATION_PROFILE);
		
			return getResult(input, translationProfile, state);
		} catch (Exception e)
		{
			finishAuthnResponseProcessing(state, e);
			throw e;
		}
		
	}
	

	private RemotelyAuthenticatedInput getRemotelyAuthenticatedInput(OAuthContext context) 
			throws AuthenticationException 
	{
		String error = context.getErrorCode();
		if (error != null)
		{
			throw new AuthenticationException("OAuth provider returned an error: " + 
					error + (context.getErrorDescription() != null ? 
							" " + context.getErrorDescription() : ""));
		}
		
		boolean openIdConnectMode = config.getProvider(context.getProviderConfigKey()).getBooleanValue(
				CustomProviderProperties.OPENID_CONNECT);
		
		Map<String, String> attributes;
		try
		{
			attributes = openIdConnectMode ? getAccessTokenAndProfileOpenIdConnect(context) :
				getAccessTokenAndProfilePlain(context);
		} catch (Exception e)
		{
			throw new AuthenticationException("Problem during user information retrieval", e);
		}

		return convertInput(context, attributes);
	}
	
	private HTTPResponse retrieveAccessTokenGeneric(OAuthContext context, String tokenEndpoint, 
			ClientAuthnMode mode) 
			throws SerializeException, IOException, URISyntaxException
	{
		String clientId = config.getProvider(context.getProviderConfigKey()).getValue(
				CustomProviderProperties.CLIENT_ID);
		String clientSecret = config.getProvider(context.getProviderConfigKey()).getValue(
				CustomProviderProperties.CLIENT_SECRET);

		ClientAuthentication clientAuthn = getClientAuthentication(clientId, clientSecret, mode);
		AuthorizationCodeGrant authzCodeGrant = new AuthorizationCodeGrant(
				new AuthorizationCode(context.getAuthzCode()), 
				new URI(responseConsumerAddress)); 
		TokenRequest request = new TokenRequest(
				new URI(tokenEndpoint),
				clientAuthn,
				authzCodeGrant);
		HTTPRequest httpRequest = CustomHTTPSRequest.wrapRequest(request.toHTTPRequest(), context, config); 
		if (log.isTraceEnabled())
		{
			String notSecretQuery = httpRequest.getQuery().replaceFirst(
					"client_secret=[^&]*", "client_secret=xxxxxx");
			log.trace("Exchanging authorization code for access token with request to: " + 
					httpRequest.getURL() + "?" + notSecretQuery);
		} else if (log.isDebugEnabled())
		{
			log.debug("Exchanging authorization code for access token with request to: " + 
					httpRequest.getURL());
		}
		HTTPResponse response = httpRequest.send();
		
		log.debug("Received answer: " + response.getStatusCode());
		if (response.getStatusCode() != 200)
			log.debug("Error received. Contents: " + response.getContent());
		else
			log.trace("Received token: " + response.getContent());
		return response;
	}
	
	private ClientAuthentication getClientAuthentication(String clientId, String clientSecret, 
			ClientAuthnMode mode)
	{
		switch (mode)
		{
		case secretPost:
			return new ClientSecretPost(new ClientID(clientId), new Secret(clientSecret));
		case secretBasic:
		default:
			return new ClientSecretBasic(new ClientID(clientId), new Secret(clientSecret));
		}
	}
	
	private Map<String, String> getAccessTokenAndProfileOpenIdConnect(OAuthContext context) throws Exception 
	{
		CustomProviderProperties providerCfg = config.getProvider(context.getProviderConfigKey());
		String discoveryEndpoint = providerCfg.getValue(CustomProviderProperties.OPENID_DISCOVERY);
		OIDCProviderMetadata providerMeta = metadataManager.getMetadata(discoveryEndpoint, providerCfg);
		String tokenEndpoint = providerCfg.getValue(CustomProviderProperties.ACCESS_TOKEN_ENDPOINT);
		if (tokenEndpoint == null)
		{
			if (providerMeta.getTokenEndpointURI() != null)
				tokenEndpoint = providerMeta.getTokenEndpointURI().toString();
			else
				throw new AuthenticationException("The access token endpoint is not set "
						+ "in provider's metadata and it is not configured manually");
		}
		
		ClientAuthnMode selectedMethod = establishOpenIDAuthnMode(providerMeta, providerCfg);
		
		HTTPResponse response = retrieveAccessTokenGeneric(context, tokenEndpoint, selectedMethod);
		OIDCTokenResponse acResponse = OIDCTokenResponse.parse(response);
		BearerAccessToken accessToken = extractAccessToken(acResponse);
		
		JWTClaimsSet accessTokenClaimsSet = acResponse.getOIDCTokens().getIDToken().getJWTClaimsSet();
		Map<String, String> ret = OpenIdUtils.toAttributes(accessTokenClaimsSet);
		
		String userInfoEndpointStr = providerCfg.getValue(CustomProviderProperties.PROFILE_ENDPOINT);
		String userInfoEndpoint = userInfoEndpointStr == null ? 
				providerMeta.getUserInfoEndpointURI().toString() : userInfoEndpointStr;

		UserProfileFetcher userAttributesFetcher = providerCfg.getUserAttributesResolver();
		if (userInfoEndpoint != null && userAttributesFetcher != null)
		{
			ret.putAll(userAttributesFetcher.fetchProfile(accessToken, userInfoEndpoint, providerCfg));
		}
		
		log.debug("Received the following attributes from the OAuth provider: " + ret);
		
		return ret;
	}
	
	private ClientAuthnMode establishOpenIDAuthnMode(OIDCProviderMetadata providerMeta,
			CustomProviderProperties providerCfg) throws AuthenticationException
	{
		ClientAuthnMode selectedMethod = ClientAuthnMode.secretBasic;
		if (providerCfg.isSet(CustomProviderProperties.CLIENT_AUTHN_MODE))
		{
			selectedMethod = providerCfg.getEnumValue(CustomProviderProperties.CLIENT_AUTHN_MODE, 
					ClientAuthnMode.class);
		} else
		{
			List<ClientAuthenticationMethod> supportedMethods = providerMeta.getTokenEndpointAuthMethods();
			if (supportedMethods != null)
			{
				selectedMethod = null;
				for (ClientAuthenticationMethod sm: supportedMethods)
				{
					if (ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(sm))
					{
						selectedMethod = ClientAuthnMode.secretPost;
						break;
					} else if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(sm))
					{
						selectedMethod = ClientAuthnMode.secretBasic;
						break;
					}
				}
				if (selectedMethod == null)
					throw new AuthenticationException("Client authentication metods supported by"
							+ " the provider (" + supportedMethods + ") do not include "
							+ "any of methods supported by Unity.");
			}
		}
		return selectedMethod;
	}
	
	private Map<String, String> getAccessTokenAndProfilePlain(OAuthContext context) throws Exception 
	{
		CustomProviderProperties providerCfg = config.getProvider(context.getProviderConfigKey());
		String tokenEndpoint = providerCfg.getValue(CustomProviderProperties.ACCESS_TOKEN_ENDPOINT);
		ClientAuthnMode selectedMethod = providerCfg.getEnumValue(CustomProviderProperties.CLIENT_AUTHN_MODE, 
					ClientAuthnMode.class);
		HTTPResponse response = retrieveAccessTokenGeneric(context, tokenEndpoint, selectedMethod);
		
		AccessTokenFormat accessTokenFormat = providerCfg.getEnumValue(CustomProviderProperties.ACCESS_TOKEN_FORMAT, 
				AccessTokenFormat.class);

		Map<String, String> ret = new HashMap<String, String>();
		BearerAccessToken accessToken;
		if (accessTokenFormat == AccessTokenFormat.standard)
		{
			AccessTokenResponse atResponse = AccessTokenResponse.parse(response);
			accessToken = extractAccessToken(atResponse);
			extractUserInfoFromStandardAccessToken(atResponse, ret);
		} else
		{
			if (response.getStatusCode() != 200)
				throw new AuthenticationException("Exchange of authorization code for access "
						+ "token failed: " + response.getContent());
			MultiMap<String> map = new MultiMap<String>();
			UrlEncoded.decodeTo(response.getContent().trim(), map, "UTF-8");
			String accessTokenVal = map.getString("access_token");
			if (accessTokenVal == null)
				throw new AuthenticationException("Access token answer received doesn't contain "
						+ "'access_token' parameter.");
			String lifetimeStr = map.getString("expires");
			if (lifetimeStr == null)
				lifetimeStr = map.getString("expires_in");
			if (lifetimeStr == null)
			{
				log.debug("AS didn't provide expiration time, assuming default value " + 
						DEFAULT_TOKEN_EXPIRATION);
				lifetimeStr = DEFAULT_TOKEN_EXPIRATION;
			}
			accessToken = new BearerAccessToken(accessTokenVal, Long.parseLong(lifetimeStr), null);
			extractUserInfoFromHttpParamsAccessToken(map, ret);
		}

		String userInfoEndpoint = providerCfg.getValue(CustomProviderProperties.PROFILE_ENDPOINT);
		UserProfileFetcher userAttributesFetcher = providerCfg.getUserAttributesResolver();
		if (userInfoEndpoint != null && userAttributesFetcher != null)
		{
			ret.putAll(userAttributesFetcher.fetchProfile(accessToken, userInfoEndpoint, providerCfg));
		}
		
		log.debug("Received the following attributes from the OAuth provider: " + ret);
		return ret;
	}
	
	
	private void extractUserInfoFromStandardAccessToken(AccessTokenResponse atResponse, Map<String, String> ret)
	{
		Map<String, Object> customParameters = atResponse.getCustomParameters();
		for (Map.Entry<String, Object> e: customParameters.entrySet())
		{
			if (attributeIgnored(e.getKey()))
				continue;
			ret.put(e.getKey(), e.getValue().toString());
		}
	}

	private void extractUserInfoFromHttpParamsAccessToken(MultiMap<String> params, Map<String, String> ret)
	{
		for (Map.Entry<String, List<String>> param: params.entrySet())
		{
			String key = param.getKey();
			List<String> values = param.getValue();
			if (attributeIgnored(key) || values.isEmpty())
				continue;
			String value = values.size() == 1 ? values.get(0) : values.toString();
			ret.put(key, value);
		}
	}
	
	private boolean attributeIgnored(String key)
	{
		return key.equals("access_token") || key.equals("expires") || key.equals("expires_in") ||
				key.equals("id_token");
	}
	
	private BearerAccessToken extractAccessToken(AccessTokenResponse atResponse) throws AuthenticationException
	{
		AccessToken accessTokenGeneric = atResponse.getTokens().getAccessToken();
		if (!(accessTokenGeneric instanceof BearerAccessToken))
		{
			throw new AuthenticationException("OAuth provider returned an access token which is not "
					+ "the bearer token, it is unsupported and most probably a problem on "
					+ "the provider side. The received token type is: " + 
					accessTokenGeneric.getType().toString());
		}
		return (BearerAccessToken) accessTokenGeneric;
	}
	
	private RemotelyAuthenticatedInput convertInput(OAuthContext context, Map<String, String> attributes)
	{
		CustomProviderProperties provCfg = config.getProvider(context.getProviderConfigKey());
		String tokenEndpoint = provCfg.getValue(CustomProviderProperties.ACCESS_TOKEN_ENDPOINT);
		String discoveryEndpoint = provCfg.getValue(CustomProviderProperties.OPENID_DISCOVERY);
		if (tokenEndpoint == null && discoveryEndpoint != null)
		{
			try
			{
				OIDCProviderMetadata providerMeta = metadataManager.getMetadata(discoveryEndpoint,
						provCfg);
				tokenEndpoint = providerMeta.getTokenEndpointURI().toString();
			} catch (Exception e)
			{
				log.warn("Can't obtain OIDC metadata", e);
			}
		}
		if (tokenEndpoint == null)
			tokenEndpoint = "unknown";

		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput(tokenEndpoint);
		for (Map.Entry<String, String> attr: attributes.entrySet())
		{
			input.addAttribute(new RemoteAttribute(attr.getKey(), attr.getValue()));
		}
		return input;
	}
}







