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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
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
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest.Builder;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import eu.unicore.util.configuration.ConfigurationException;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthnContext;
import pl.edu.icm.unity.engine.api.authn.AuthnContext.Protocol;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationException;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RedirectedAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.authn.remote.SharedRemoteAuthenticationContextStore;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.URIBuilderFixer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.oauth.client.profile.ProfileFetcherUtils;
import pl.edu.icm.unity.oauth.oidc.metadata.OAuthDiscoveryMetadataCache;
import pl.edu.icm.unity.types.authn.ExpectedIdentity;
import pl.edu.icm.unity.types.authn.ExpectedIdentity.IdentityExpectation;
import pl.edu.icm.unity.types.authn.IdPInfo;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;


/**
 * Binding independent OAuth 2 logic. Creates authZ requests, validates response (OAuth authorization grant)
 * performs subsequent call to AS to get resource owner's (authenticated user) information.
 *   
 * @author K. Benedyczak
 */
@PrototypeComponent
public class OAuth2Verificator extends AbstractRemoteVerificator implements OAuthExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuth2Verificator.class);
	public static final String NAME = "oauth2";
	public static final String DESC = "Handles OAuth2 tokens obtained from remote OAuth providers. "
			+ "Queries about additional user information.";
	public static final String DEFAULT_TOKEN_EXPIRATION = "3600";
	
	
	private OAuthClientProperties config;
	private final String responseConsumerAddress;
	private final OAuthContextsManagement contextManagement;
	private final PKIManagement pkiManagement;
	private final MessageSource msg;
	private OAuthDiscoveryMetadataCache metadataManager;
	
	@Autowired
	public OAuth2Verificator(MessageSource msg, AdvertisedAddressProvider advertisedAddrProvider,
			SharedEndpointManagement sharedEndpointManagement,
			OAuthContextsManagement contextManagement,
			PKIManagement pkiManagement,
			RemoteAuthnResultTranslator processor,
			OAuthDiscoveryMetadataCache metadataManager)
	{
		super(NAME, DESC, OAuthExchange.ID, processor);
		URL baseAddress = advertisedAddrProvider.get();
		String baseContext = sharedEndpointManagement.getBaseContextPath();
		this.responseConsumerAddress = baseAddress + baseContext + ResponseConsumerServlet.PATH;
		this.contextManagement = contextManagement;
		this.pkiManagement = pkiManagement;
		this.msg = msg;
		this.metadataManager = metadataManager;
	}

	@Override
	public String getSerializedConfiguration()
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
	public void setSerializedConfiguration(String source)
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			config = new OAuthClientProperties(properties, pkiManagement);			
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
	public OAuthContext createRequest(String providerKey, Optional<ExpectedIdentity> expectedIdentity, 
			AuthenticationStepContext authnStepContext, 
			LoginMachineDetails initialLoginMachine, 
			String ultimateReturnURL,
			AuthenticationTriggeringContext authnTriggeringContext) 
			throws URISyntaxException, ParseException, IOException
	{
		CustomProviderProperties providerCfg = config.getProvider(providerKey); 
		String clientId = providerCfg.getValue(CustomProviderProperties.CLIENT_ID);
		String authzEndpoint = providerCfg.getValue(CustomProviderProperties.PROVIDER_LOCATION);
		
		String scopes = providerCfg.getValue(CustomProviderProperties.SCOPES);
		boolean openidMode = providerCfg.getBooleanValue(CustomProviderProperties.OPENID_CONNECT);

		RedirectedAuthnState baseAuthnContext = new RedirectedAuthnState(authnStepContext, this::processResponse, 
				initialLoginMachine, ultimateReturnURL, 
				authnTriggeringContext);
		OAuthContext context = new OAuthContext(baseAuthnContext);
		AuthorizationRequest req;
		if (openidMode)
		{
			if (Strings.isNullOrEmpty(authzEndpoint))
			{	
				OIDCProviderMetadata providerMeta = metadataManager.getMetadata(providerCfg.generateMetadataRequest());
				if (providerMeta.getAuthorizationEndpointURI() == null)
					throw new ConfigurationException("The authorization endpoint address is not set and"
							+ " it is not available in the discovered OpenID Provider metadata.");
				authzEndpoint = providerMeta.getAuthorizationEndpointURI().toString();
			}
			Builder builder = new AuthenticationRequest.Builder(new ResponseType(ResponseType.Value.CODE), 
					Scope.parse(scopes), new ClientID(clientId),
					new URI(responseConsumerAddress));
			builder.state(new State(context.getRelayState()))
				.endpointURI(new URI(authzEndpoint));
			if (expectedIdentity.isPresent())
			{
				builder.loginHint(expectedIdentity.get().getIdentity());
				context.setExpectedIdentity(expectedIdentity.get());
			}
			req = builder.build();
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
		
		URIBuilder uriBuilder = URIBuilderFixer.newInstance(req.toURI());
		uriBuilder.addParameters(providerCfg.getAdditionalAuthzParams());
		context.setRequest(req, uriBuilder.build(), providerKey);
		contextManagement.addAuthnContext(context);
		return context;
	}

	private AuthenticationResult processResponse(RedirectedAuthnState remoteAuthnState)
	{
		try
		{
			return verifyOAuthAuthzResponse((OAuthContext) remoteAuthnState);
		} catch (Exception e)
		{
			log.error("Runtime error during OAuth2 response processing or principal mapping", e);
			return RemoteAuthenticationResult.failed(null, e, new ResolvableError("OAuth2Retrieval.authnFailedError"));
		}
	}

	
	/**
	 * The real OAuth workhorse. The authz code response verification needs not to be done: the state is 
	 * correct as otherwise there would be no match with the {@link OAuthContext}. However we need to
	 * use the authz code to retrieve access token. The access code may include everything we need. But it 
	 * may also happen that we need to perform one more query to obtain additional profile information.
	 */
	private AuthenticationResult verifyOAuthAuthzResponse(OAuthContext context)
	{
		try
		{
			RemotelyAuthenticatedInput input = getRemotelyAuthenticatedInput(context);
			verifyExpectedIdentity(input, context.getExpectedIdentity());
			CustomProviderProperties providerProps = config.getProvider(context.getProviderConfigKey());
			TranslationProfile profile = getTranslationProfile(
					providerProps,
					CommonWebAuthnProperties.TRANSLATION_PROFILE,
					CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE);
			
			String regFormForUnknown = providerProps.getValue(CommonWebAuthnProperties.REGISTRATION_FORM);
			boolean enableAssociation = providerProps.isSet(CommonWebAuthnProperties.ENABLE_ASSOCIATION) ?
					providerProps.getBooleanValue(CommonWebAuthnProperties.ENABLE_ASSOCIATION) :
					config.getBooleanValue(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION);
			return getResult(input, 
					profile, 
					context.getAuthenticationTriggeringContext().isSandboxTriggered(), 
					regFormForUnknown, 
					enableAssociation);
		} catch (UnexpectedIdentityException uie)
		{
			return RemoteAuthenticationResult.failed(null, uie,
					new ResolvableError("OAuth2Retrieval.unexpectedUser", uie.expectedIdentity));
		} catch (RemoteAuthenticationException e)
		{
			log.info("OAuth2 authorization code verification or processing failed", e);
			return RemoteAuthenticationResult.failed(e.getResult().getRemotelyAuthenticatedPrincipal(), e, 
					new ResolvableError("OAuth2Retrieval.authnFailedError"));
		}
		
	}
	
	private void verifyExpectedIdentity(RemotelyAuthenticatedInput input, ExpectedIdentity expectedIdentity)
	{
		if (expectedIdentity == null)
			return;
		if (expectedIdentity.getExpectation() == IdentityExpectation.HINT)
			return;
		String identity = expectedIdentity.getIdentity();
		if (input.getIdentities().values().stream()
				.filter(ri -> ri.getName().equals(identity))
				.findAny().isPresent())
			return;
		if (input.getAttributes().values().stream()
				.filter(ra -> ra.getName().equals(UserInfo.EMAIL_CLAIM_NAME))
				.filter(ra -> ra.getValues().contains(identity))
				.findAny().isPresent())
			return;
		log.warn("Failing OAuth authentication as expected&mandatory identity {} was not found "
				+ "in received user data: {}", identity, input.getTextDump());
		throw new UnexpectedIdentityException(identity);
	}

	private RemotelyAuthenticatedInput getRemotelyAuthenticatedInput(OAuthContext context) 
			throws RemoteAuthenticationException 
	{
		String error = context.getErrorCode();
		if (error != null)
		{
			throw new RemoteAuthenticationException("OAuth provider returned an error: " + 
					error + (context.getErrorDescription() != null ? 
							" " + context.getErrorDescription() : ""));
		}
		
		boolean openIdConnectMode = config.getProvider(context.getProviderConfigKey()).getBooleanValue(
				CustomProviderProperties.OPENID_CONNECT);
		
		AttributeFetchResult attributes;
		try
		{
			attributes = openIdConnectMode ? getAccessTokenAndProfileOpenIdConnect(context) :
				getAccessTokenAndProfilePlain(context);
		} catch (Exception e)
		{
			throw new RemoteAuthenticationException("Problem during user information retrieval", e);
		}

		return  convertInput(context, attributes, openIdConnectMode);
	}
	
	private AccessTokenFormat getAccessTokenFormat(OAuthContext context)
	{
		CustomProviderProperties providerCfg = config
				.getProvider(context.getProviderConfigKey());
		return providerCfg.getEnumValue(CustomProviderProperties.ACCESS_TOKEN_FORMAT,
				AccessTokenFormat.class);

	}

	private HTTPResponse retrieveAccessTokenGeneric(OAuthContext context, String tokenEndpoint, 
			ClientAuthnMode mode) 
			throws IOException, URISyntaxException
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
		CustomProviderProperties providerCfg = config.getProvider(context.getProviderConfigKey());
		
		HTTPRequest httpRequest = new HttpRequestConfigurer()
				.secureRequest(request.toHTTPRequest(), providerCfg.getValidator(), providerCfg.getHostNameCheckingMode()); 
		if (getAccessTokenFormat(context) == AccessTokenFormat.standard)
			httpRequest.setAccept(MediaType.APPLICATION_JSON);
		
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
		
		log.debug("Received answer: {}", response.getStatusCode());
		if (response.getStatusCode() != 200)
			log.warn("Error received. Contents: {}", response.getContent());
		else
			log.trace("Received token: {}", response.getContent().trim());
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
	
	private AttributeFetchResult getAccessTokenAndProfileOpenIdConnect(OAuthContext context) throws Exception 
	{
		CustomProviderProperties providerCfg = config.getProvider(context.getProviderConfigKey());
		OIDCProviderMetadata providerMeta = metadataManager.getMetadata(providerCfg.generateMetadataRequest());
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
		
		JWT idToken = acResponse.getOIDCTokens().getIDToken();
		if (idToken == null)
			throw new AuthenticationException("Id token was not returned by the authorization server");
		JWTClaimsSet accessTokenClaimsSet = idToken.getJWTClaimsSet();
		Map<String, List<String>> accessTokenAttributes = ProfileFetcherUtils.convertToAttributes(
				new JSONObject(accessTokenClaimsSet.getClaims()));
		
		List<String> userInfoEndpoints = providerCfg.getUserInfoEndpoints();
		if (userInfoEndpoints.isEmpty() && providerMeta.getUserInfoEndpointURI() != null) 
			userInfoEndpoints.add(providerMeta.getUserInfoEndpointURI().toString());

		return fetchUserAttributes(providerCfg, accessToken, accessTokenAttributes, userInfoEndpoints);
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
	
	private AttributeFetchResult getAccessTokenAndProfilePlain(OAuthContext context) throws Exception 
	{
		CustomProviderProperties providerCfg = config.getProvider(context.getProviderConfigKey());
		String tokenEndpoint = providerCfg.getValue(CustomProviderProperties.ACCESS_TOKEN_ENDPOINT);
		ClientAuthnMode selectedMethod = providerCfg.getEnumValue(CustomProviderProperties.CLIENT_AUTHN_MODE, 
					ClientAuthnMode.class);
		HTTPResponse response = retrieveAccessTokenGeneric(context, tokenEndpoint, selectedMethod);
		Map<String, List<String>> accessTokenAttributes = new HashMap<>();
		BearerAccessToken accessToken;
		if (getAccessTokenFormat(context) == AccessTokenFormat.standard)
		{
			JSONObject jsonResp = response.getContentAsJSONObject();
			if (!jsonResp.containsKey("token_type"))
				jsonResp.put("token_type", AccessTokenType.BEARER.getValue());
			AccessTokenResponse atResponse = AccessTokenResponse.parse(jsonResp);
			accessToken = extractAccessToken(atResponse);
			extractUserInfoFromStandardAccessToken(atResponse, accessTokenAttributes);
		} else
		{
			if (response.getStatusCode() != 200)
				throw new AuthenticationException("Exchange of authorization code for access "
						+ "token failed: " + response.getContent());
			MultiMap<String> map = new MultiMap<>();
			UrlEncoded.decodeTo(response.getContent().trim(), map, StandardCharsets.UTF_8);
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
			extractUserInfoFromHttpParamsAccessToken(map, accessTokenAttributes);
		}

		List<String> userInfoEndpoints = providerCfg.getUserInfoEndpoints();
		return fetchUserAttributes(providerCfg, accessToken, accessTokenAttributes, userInfoEndpoints);
	}
	
	private AttributeFetchResult fetchUserAttributes(CustomProviderProperties providerCfg, 
			BearerAccessToken accessToken, Map<String, List<String>> baseAttributes,
			List<String> userInfoEndpoints) throws Exception
	{
		UserProfileFetcher userAttributesFetcher = providerCfg.getUserAttributesResolver();
		AttributeFetchResult fetchRet = new AttributeFetchResult();
		if (userAttributesFetcher != null)
		{
			for (String userInfoEndpoint: userInfoEndpoints)
			{
				AttributeFetchResult fetchSingle = userAttributesFetcher.fetchProfile(accessToken, 
					userInfoEndpoint, providerCfg, baseAttributes);
				fetchRet = fetchRet.mergeWith(fetchSingle);
			}
		}
		fetchRet.getAttributes().putAll(baseAttributes); //minor bug - those won't be visible as rawAttribtues.
		log.debug("Received the following attributes from the OAuth provider: {}", fetchRet.getAttributes());
		return fetchRet;
	}
	
	private void extractUserInfoFromStandardAccessToken(AccessTokenResponse atResponse, Map<String, List<String>> ret)
	{
		Map<String, Object> customParameters = atResponse.getCustomParameters();
		for (Map.Entry<String, Object> e: customParameters.entrySet())
		{
			if (attributeIgnored(e.getKey()))
				continue;
			ret.put(e.getKey(), Arrays.asList(e.getValue().toString()));
		}
	}

	private void extractUserInfoFromHttpParamsAccessToken(MultiMap<String> params, Map<String, List<String>> ret)
	{
		for (Map.Entry<String, List<String>> param: params.entrySet())
		{
			String key = param.getKey();
			List<String> values = param.getValue();
			if (attributeIgnored(key) || values.isEmpty())
				continue;
			ret.put(key, values);
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
	
	private RemotelyAuthenticatedInput convertInput(OAuthContext context, AttributeFetchResult attributes, boolean openIdConnectMode)
	{
		CustomProviderProperties provCfg = config.getProvider(context.getProviderConfigKey());
		String tokenEndpoint = provCfg.getValue(CustomProviderProperties.ACCESS_TOKEN_ENDPOINT);
		String discoveryEndpoint = provCfg.getValue(CustomProviderProperties.OPENID_DISCOVERY);
		if (tokenEndpoint == null && discoveryEndpoint != null)
		{
			try
			{
				OIDCProviderMetadata providerMeta = metadataManager.getMetadata(provCfg.generateMetadataRequest());
				tokenEndpoint = providerMeta.getTokenEndpointURI().toString();
			} catch (Exception e)
			{
				log.warn("Can't obtain OIDC metadata", e);
			}
		}
		if (tokenEndpoint == null)
			tokenEndpoint = "unknown";

		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput(tokenEndpoint);
		for (Map.Entry<String, List<String>> attr: attributes.getAttributes().entrySet())
		{
			input.addAttribute(new RemoteAttribute(attr.getKey(), attr.getValue().toArray()));
			if (attr.getKey().equals("sub") && !attr.getValue().isEmpty())
				input.addIdentity(new RemoteIdentity(attr.getValue().get(0), "sub"));
		}
		input.setRawAttributes(attributes.getRawAttributes());
		input.setAuthnContext(getAuthnContext(attributes, openIdConnectMode));
		return input;
	}
	
	private AuthnContext getAuthnContext(AttributeFetchResult attributes, boolean openIdConnectMode)
	{
		return new AuthnContext(openIdConnectMode ? Protocol.OIDC : Protocol.OIDC,
				openIdConnectMode ? attributes.getAttributes()
						.get("iss")
						.get(0) : AuthnContext.UNDEFINED_IDP,
				getAcr(attributes));
	}

	private List<String> getAcr(AttributeFetchResult attributes)
	{
		return attributes.getAttributes()
				.get("acr") != null ? List.of(
						attributes.getAttributes()
								.get("acr")
								.get(0))
						: List.of();
	}
	
	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Remote;
	}
	
	@Override
	public List<IdPInfo> getIdPs()
	{
		List<IdPInfo> providers = new ArrayList<>();
		Set<String> keys = config.getStructuredListKeys(OAuthClientProperties.PROVIDERS);
		for (String key : keys)
		{
			CustomProviderProperties providerProps = config.getProvider(key);
			String idpKey = key.substring(OAuthClientProperties.PROVIDERS.length(), key.length() - 1);
			if (config.getProvider(key).getBooleanValue(CustomProviderProperties.OPENID_CONNECT))
			{
				extractIdPInfoFromOIDCProvider(key, idpKey, providerProps).ifPresent(i -> providers.add(i));

			} else
			{
				IdPInfo providerInfo = IdPInfo.builder()
						.withId(config.getProvider(key).getValue(CustomProviderProperties.ACCESS_TOKEN_ENDPOINT))
						.withConfigId(idpKey)
						.withDisplayedName(config.getProvider(key).getLocalizedStringWithoutFallbackToDefault(msg,
								CustomProviderProperties.PROVIDER_NAME))
						.build();
				providers.add(providerInfo);
			}
		}
		return providers;
	}
	
	private Optional<IdPInfo> extractIdPInfoFromOIDCProvider(String key, String idpKey,
			CustomProviderProperties providerProps)

	{
		
		OIDCProviderMetadata metadata;
		try
		{
			metadata = metadataManager.getMetadata(providerProps.generateMetadataRequest());
		} catch (Exception e)
		{
			log.warn("Can't obtain OIDC metadata", e);
			return Optional.empty();
		}

		return Optional.of(IdPInfo.builder().withId(metadata.getTokenEndpointURI().toString()).withConfigId(idpKey)
				.withDisplayedName(config.getProvider(key).getLocalizedStringWithoutFallbackToDefault(msg,
						CustomProviderProperties.PROVIDER_NAME))
				.build());
	}
	
	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<OAuth2Verificator> factory, 
				SharedEndpointManagement sharedEndpointManagement,
				OAuthContextsManagement contextManagement,
				SharedRemoteAuthenticationContextStore remoteAuthnContextStore) throws EngineException
		{
			super(NAME, DESC, factory);
			
			ServletHolder servlet = new ServletHolder(new ResponseConsumerServlet(contextManagement, 
					remoteAuthnContextStore));
			sharedEndpointManagement.deployInternalEndpointServlet(
					ResponseConsumerServlet.PATH, servlet, false);
		}
	}	
}







