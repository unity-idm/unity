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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import net.minidev.json.JSONObject;

import org.apache.log4j.Logger;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
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
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.OIDCAccessTokenResponse;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.utils.Log;


/**
 * Binding independent OAuth 2 logic. Creates authZ requests, validates response (OAuth authorization grant)
 * performs subsequent call to AS to get resource owner's (authenticated user) information.
 *   
 * @author K. Benedyczak
 */
public class OAuth2Verificator extends AbstractRemoteVerificator implements OAuthExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuth2Verificator.class);
	private OAuthClientProperties config;
	private String responseConsumerAddress;
	private OAuthContextsManagement contextManagement;
	private OpenIdProviderMetadataManager metadataManager;
	
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
				OIDCProviderMetadata providerMeta = metadataManager.getMetadata(discoveryEndpoint);
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
			attributes = openIdConnectMode ? getUserInfoWithOpenIdConnect(context) :
				getUserInfoWithPlainOAuth2(context);
		} catch (SerializeException | ParseException | IOException | URISyntaxException
				| java.text.ParseException e)
		{
			throw new AuthenticationException("Problem during user information retrieval", e);
		}

		RemotelyAuthenticatedInput input = convertInput(context, attributes);

		String translationProfile = config.getProvider(context.getProviderConfigKey()).getValue( 
				CustomProviderProperties.TRANSLATION_PROFILE);
		
		return getResult(input, translationProfile);
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
		HTTPRequest httpRequest = request.toHTTPRequest(); 
		if (log.isTraceEnabled())
		{
			String notSecretQuery = httpRequest.getQuery().replaceFirst(
					"client_secret=[^&]*", "client_secret=xxxxxx");
			log.trace("Exchanging authorization code for access token with request to: " + httpRequest.getURL() + 
				"?" + notSecretQuery);
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
	
	private Map<String, String> getUserInfoWithOpenIdConnect(OAuthContext context) 
			throws AuthenticationException, SerializeException, IOException, URISyntaxException, 
			ParseException, java.text.ParseException 
	{
		CustomProviderProperties providerCfg = config.getProvider(context.getProviderConfigKey());
		String discoveryEndpoint = providerCfg.getValue(CustomProviderProperties.OPENID_DISCOVERY);
		OIDCProviderMetadata providerMeta = metadataManager.getMetadata(discoveryEndpoint);
		String tokenEndpoint = providerCfg.getValue(CustomProviderProperties.ACCESS_TOKEN_ENDPOINT);
		if (tokenEndpoint == null)
		{
			if (providerMeta.getTokenEndpointURI() != null)
				tokenEndpoint = providerMeta.getTokenEndpointURI().toString();
			else
				throw new AuthenticationException("The access token endpoint is not provided in provider's metadata"
						+ " and it is not configured manually");
		}
		ClientAuthnMode selectedMethod = providerCfg.getEnumValue(CustomProviderProperties.CLIENT_AUTHN_MODE, 
				ClientAuthnMode.class);
		if (selectedMethod == null)
		{
			List<ClientAuthenticationMethod> supportedMethods = providerMeta.getTokenEndpointAuthMethods();
			for (ClientAuthenticationMethod sm: supportedMethods)
			{
				if ("client_secret_post".equals(sm.getValue()))
				{
					selectedMethod = ClientAuthnMode.secretPost;
					break;
				} else if ("client_secret_basic".equals(sm.getValue()))
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
		HTTPResponse response = retrieveAccessTokenGeneric(context, tokenEndpoint, selectedMethod);
		OIDCAccessTokenResponse acResponse = OIDCAccessTokenResponse.parse(response);
		BearerAccessToken accessToken = extractAccessToken(acResponse);
		
		Map<String, String> ret = new HashMap<String, String>();
		toAttributes(acResponse.getIDToken().getJWTClaimsSet(), ret);
		
		String userInfoEndpointStr = providerCfg.getValue(CustomProviderProperties.PROFILE_ENDPOINT);
		URI userInfoEndpoint = userInfoEndpointStr == null ? providerMeta.getUserInfoEndpointURI() : 
			new URI(userInfoEndpointStr);

		if (userInfoEndpoint != null)
		{
			fetchOpenIdUserInfo(accessToken, userInfoEndpoint, ret);
		}
		
		log.debug("Received the following attributes from the OAuth provider: " + ret);
		
		return ret;
	}
	
	private void fetchOpenIdUserInfo(BearerAccessToken accessToken, URI userInfoEndpoint, Map<String, String> ret) 
			throws AuthenticationException, SerializeException, IOException, ParseException, java.text.ParseException
	{
		UserInfoRequest uiRequest = new UserInfoRequest(userInfoEndpoint, accessToken);
		HTTPResponse uiHttpResponse = uiRequest.toHTTPRequest().send();
		UserInfoResponse uiResponse = UserInfoResponse.parse(uiHttpResponse);
		if (uiResponse instanceof UserInfoErrorResponse)
		{
			String code = uiHttpResponse.getContent();
			throw new AuthenticationException("Authentication was successful, but an error "
					+ "occurred during user information endpoint query: " + 
					code);
		}
		UserInfoSuccessResponse uiResponseS = (UserInfoSuccessResponse) uiResponse;
		if (uiResponseS.getUserInfoJWT() != null)
		{
			ReadOnlyJWTClaimsSet claimSet = uiResponseS.getUserInfoJWT().getJWTClaimsSet();
			toAttributes(claimSet, ret);
		} else
		{
			UserInfo ui = uiResponseS.getUserInfo();
			JWTClaimsSet claimSet = ui.toJWTClaimsSet();
			toAttributes(claimSet, ret);
		}
	}
	
	private void fetchUserInfo(BearerAccessToken accessToken, ClientAuthnMode selectedMethod,
			String userInfoEndpoint, Map<String, String> ret) 
					throws AuthenticationException, IOException, ParseException
	{
		HTTPRequest httpReq = new HTTPRequest(Method.GET, new URL(userInfoEndpoint));
		if (selectedMethod == ClientAuthnMode.secretPost)
			httpReq.setQuery("access_token=" + accessToken.getValue());
		else
			httpReq.setAuthorization(accessToken.toAuthorizationHeader());
		
		HTTPResponse resp = httpReq.send();
		
		if (resp.getStatusCode() != 200)
		{
			throw new AuthenticationException("Authentication was successful "
					+ "but there was a problem fetching user's profile information: " + 
					resp.getContent());
		}
		if (!"application/json".equals(resp.getContentType().getBaseType().toString()))
			throw new AuthenticationException("Authentication was successful "
					+ "but there was a problem fetching user's profile information. "
					+ "It has non-JSON content type: " + resp.getContentType());
		
		JSONObject profile = resp.getContentAsJSONObject();
		
		for (Entry<String, Object> entry: profile.entrySet())
		{
			ret.put(entry.getKey(), entry.getValue().toString());
		}
	}
	
	private void toAttributes(ReadOnlyJWTClaimsSet claimSet, Map<String, String> attributes)
	{
		Map<String, Object> claims = claimSet.getAllClaims();
		for (Map.Entry<String, Object> claim: claims.entrySet())
		{
			if (claim.getValue() != null)
				attributes.put(claim.getKey(), claim.getValue().toString());
			else
				attributes.put(claim.getKey(), "");
		}
	}
	
	private Map<String, String> getUserInfoWithPlainOAuth2(OAuthContext context) 
			throws SerializeException, IOException, URISyntaxException, ParseException, AuthenticationException 
	{
		CustomProviderProperties providerCfg = config.getProvider(context.getProviderConfigKey());
		String tokenEndpoint = providerCfg.getValue(CustomProviderProperties.ACCESS_TOKEN_ENDPOINT);
		ClientAuthnMode selectedMethod = providerCfg.getEnumValue(CustomProviderProperties.CLIENT_AUTHN_MODE, 
					ClientAuthnMode.class);
		HTTPResponse response = retrieveAccessTokenGeneric(context, tokenEndpoint, selectedMethod);
		AccessTokenResponse atResponse = AccessTokenResponse.parse(response);
		BearerAccessToken accessToken = extractAccessToken(atResponse);

		Map<String, String> ret = new HashMap<String, String>();
		String userInfoEndpoint = providerCfg.getValue(CustomProviderProperties.PROFILE_ENDPOINT);
		if (userInfoEndpoint != null)
		{
			fetchUserInfo(accessToken, selectedMethod, userInfoEndpoint, ret);
		}
		
		log.debug("Received the following attributes from the OAuth provider: " + ret);
		return ret;
	}
	
	private BearerAccessToken extractAccessToken(AccessTokenResponse atResponse) throws AuthenticationException
	{
		AccessToken accessTokenGeneric = atResponse.getAccessToken();
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
		String tokenEndpoint = config.getProvider(context.getProviderConfigKey()).getValue( 
				CustomProviderProperties.ACCESS_TOKEN_ENDPOINT);
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput(tokenEndpoint);
		for (Map.Entry<String, String> attr: attributes.entrySet())
		{
			input.addAttribute(new RemoteAttribute(attr.getKey(), attr.getValue()));
		}
		return input;
	}
}







