/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import static pl.edu.icm.unity.oauth.client.URLObfuscator.obfuscateURLParams;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.nimbusds.jose.JOSEException;
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
import jakarta.ws.rs.core.MediaType;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.ExpectedIdentity;
import pl.edu.icm.unity.base.authn.ExpectedIdentity.IdentityExpectation;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.IdPInfo;
import pl.edu.icm.unity.engine.api.authn.IdPInfo.IdpGroup;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationException;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RedirectedAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.authn.remote.SharedRemoteAuthenticationContextStore;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.URIBuilderFixer;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMethod;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.OAuthClientConfiguration;
import pl.edu.icm.unity.oauth.client.config.OAuthClientConfigurationParser;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderConfiguration;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderKey;
import pl.edu.icm.unity.oauth.client.config.OAuthProviders;
import pl.edu.icm.unity.oauth.client.federation.OAuthFederationEntityStatementConfig;
import pl.edu.icm.unity.oauth.client.federation.OAuthFederationEntityStatementServlet;
import pl.edu.icm.unity.oauth.client.federation.OAuthFederationMetadataManager;
import pl.edu.icm.unity.oauth.client.federation.OAuthFederationProvidersManager;
import pl.edu.icm.unity.oauth.client.profile.ProfileFetcherConfig;
import pl.edu.icm.unity.oauth.client.profile.ProfileFetcherUtils;
import pl.edu.icm.unity.oauth.oidc.metadata.OAuthDiscoveryMetadataCache;
import pl.edu.icm.unity.oauth.oidc.metadata.OIDCMetadataRequest;


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

	private final InstanceId instanceId = new InstanceId();
	private OAuthClientConfiguration config;
	private final String responseConsumerAddress;
	private final String federationEntityBaseUrl;
	private final OAuthContextsManagement contextManagement;
	private final PKIManagement pkiManagement;
	private final ClientAuthenticationFactory clientAuthnFactory;
	private final OAuthDiscoveryMetadataCache metadataManager;
	private final OAuthRemoteAuthenticationInputAssembler remoteAuthenticationInputAssembler;
	private final OAuthFederationMetadataManager federationManager;
	private final OAuthFederationProvidersManager federationProvidersManager;
	private final OAuthClientConfigurationParser configurationParser;

	@Autowired
	public OAuth2Verificator(AdvertisedAddressProvider advertisedAddrProvider,
			SharedEndpointManagement sharedEndpointManagement,
			OAuthContextsManagement contextManagement,
			PKIManagement pkiManagement,
			RemoteAuthnResultTranslator processor,
			OAuthDiscoveryMetadataCache metadataManager,
			OAuthRemoteAuthenticationInputAssembler remoteAuthenticationInputAssembler,
			OAuthFederationMetadataManager federationManager,
			OAuthFederationProvidersManager federationProvidersManager,
			OAuthClientConfigurationParser configurationParser)
	{
		super(NAME, DESC, OAuthExchange.ID, processor);
		URL baseAddress = advertisedAddrProvider.get();
		String baseContext = sharedEndpointManagement.getBaseContextPath();
		this.responseConsumerAddress = baseAddress + baseContext + ResponseConsumerServlet.PATH;
		this.federationEntityBaseUrl = baseAddress + baseContext + OAuthFederationEntityStatementServlet.PATH;
		this.contextManagement = contextManagement;
		this.pkiManagement = pkiManagement;
		this.clientAuthnFactory = new ClientAuthenticationFactory(pkiManagement);
		this.metadataManager = metadataManager;
		this.remoteAuthenticationInputAssembler = remoteAuthenticationInputAssembler;
		this.federationManager = federationManager;
		this.federationProvidersManager = federationProvidersManager;
		this.configurationParser = configurationParser;
	}


	@Override
	public String getSerializedConfiguration()
	{
		StringWriter sbw = new StringWriter();
		try
		{
			config.getRawProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize OAuth2 verificator configuration", e);
		}
		return sbw.toString();
	}

	@Override
	public void setSerializedConfiguration(String source)
	{
		config = configurationParser.parse(source);
		updateFederationConfiguration();
		if (instanceName != null)
			federationProvidersManager.setConfiguration(instanceName,
					federationEntityBaseUrl + "/" + instanceName, config, instanceId);
	}

	@Override
	public OAuthProviders getProviders()
	{
		if (instanceName != null)
			return federationProvidersManager.getCombinedProviders(instanceName);
		return config.providers();
	}

	@Override
	public void destroy()
	{
		if (instanceName != null)
		{
			federationManager.updateConfiguration(instanceName, null, instanceId);
			federationProvidersManager.removeConfiguration(instanceName, instanceId);
		}
	}

	private void updateFederationConfiguration()
	{
		if (instanceName == null)
			return;
		if (!config.federation.enabled)
		{
			federationManager.updateConfiguration(instanceName, null, instanceId);
			return;
		}

		String federationCredName = config.federation.credential;
		String authCredName = config.authenticationCredential;

		if (Strings.isNullOrEmpty(federationCredName))
		{
			log.warn("Federation membership enabled but no federation credential configured for authenticator: {}",
					instanceName);
			federationManager.updateConfiguration(instanceName, null, instanceId);
			return;
		}

		try
		{
			eu.emi.security.authn.x509.X509Credential federationCred =
					pkiManagement.getCredential(federationCredName);
			eu.emi.security.authn.x509.X509Credential authCred =
					Strings.isNullOrEmpty(authCredName) ? null : pkiManagement.getCredential(authCredName);

			String entityId = federationEntityBaseUrl + "/" + instanceName;
			String superiorEntityId = config.federation.superiorEntityId;
			long metadataValidity = config.federation.metadataValidity;

			OAuthFederationEntityStatementConfig federationConfig = new OAuthFederationEntityStatementConfig(
					entityId, federationCred, authCred, responseConsumerAddress, superiorEntityId, metadataValidity);
			federationManager.updateConfiguration(instanceName, federationConfig, instanceId);
		} catch (EngineException e)
		{
			log.error("Failed to configure federation for authenticator: {}", instanceName, e);
			federationManager.updateConfiguration(instanceName, null, instanceId);
		}
	}

	@Override
	public OAuthClientConfiguration getSettings()
	{
		return config;
	}

	@Override
	public OAuthContext createRequest(OAuthProviderKey providerKey, Optional<ExpectedIdentity> expectedIdentity,
			AuthenticationStepContext authnStepContext,
			LoginMachineDetails initialLoginMachine,
			String ultimateReturnURL,
			AuthenticationTriggeringContext authnTriggeringContext)
			throws URISyntaxException, ParseException, IOException
	{
		OAuthProviderConfiguration providerCfg = getProviders().get(providerKey);
		String clientId = providerCfg.clientId;
		String authzEndpoint = providerCfg.authorizationEndpoint;
		String scopes = providerCfg.scopes;
		boolean openidMode = providerCfg.openIdConnect;

		RedirectedAuthnState baseAuthnContext = new RedirectedAuthnState(authnStepContext, this::processResponse,
				initialLoginMachine, ultimateReturnURL,
				authnTriggeringContext);
		OAuthContext context = new OAuthContext(baseAuthnContext);
		AuthorizationRequest req;
		if (openidMode)
		{
			if (Strings.isNullOrEmpty(authzEndpoint))
			{
				OIDCProviderMetadata providerMeta = metadataManager.getMetadata(buildMetadataRequest(providerCfg));
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
			new AuthenticationRequestACRBuilder(builder).addACR(providerCfg, authnStepContext.signInInProgressContext.acr());

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
		uriBuilder.addParameters(providerCfg.additionalAuthzParams);
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
			OAuthProviderConfiguration providerCfg = getProviders().get(context.getProviderConfigKey());
			TranslationProfile profile = providerCfg.translationProfile;
			String regFormForUnknown = providerCfg.registrationForm;
			boolean enableAssociation = providerCfg.enableAssociation;
			return getResult(input, profile,
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

		OAuthProviderConfiguration providerCfg = getProviders().get(context.getProviderConfigKey());
		boolean openIdConnectMode = providerCfg.openIdConnect;

		AttributeFetchResult attributes;
		try
		{
			attributes = openIdConnectMode ? getAccessTokenAndProfileOpenIdConnect(context, providerCfg) :
				getAccessTokenAndProfilePlain(context, providerCfg);
		} catch (Exception e)
		{
			throw new RemoteAuthenticationException("Problem during user information retrieval", e);
		}

		return remoteAuthenticationInputAssembler.convertInput(providerCfg, context, attributes, openIdConnectMode);
	}

	private HTTPResponse retrieveAccessTokenGeneric(OAuthContext context, OAuthProviderConfiguration providerCfg,
			String tokenEndpoint, ClientAuthnMode mode)
			throws IOException, URISyntaxException, EngineException, JOSEException
	{
		URI tokenEndpointURI = new URI(tokenEndpoint);
		ClientAuthentication clientAuthn = clientAuthnFactory.build(providerCfg, tokenEndpointURI, mode);
		AuthorizationCodeGrant authzCodeGrant = new AuthorizationCodeGrant(
				new AuthorizationCode(context.getAuthzCode()),
				new URI(responseConsumerAddress));
		TokenRequest request = new TokenRequest(
				tokenEndpointURI,
				clientAuthn,
				authzCodeGrant, null);

		HTTPRequest httpRequest = new HttpRequestConfigurer()
				.secureRequest(request.toHTTPRequest(), providerCfg.validator,
						providerCfg.hostNameCheckingMode);
		if (providerCfg.accessTokenFormat == AccessTokenFormat.standard)
			httpRequest.setAccept(MediaType.APPLICATION_JSON);

		log.debug("Exchanging authorization code for access token with request to: {}",
					obfuscateURLParams(httpRequest.getURL()));
		HTTPResponse response = httpRequest.send();

		log.debug("Received answer: {}", response.getStatusCode());
		if (response.getStatusCode() != 200)
			log.warn("Error received. Contents: {}", response.getBody());
		else
			log.trace("Received token: {}", Optional.ofNullable(response.getBody())
					.map(r -> r.trim())
					.orElse(null));
		return response;
	}

	private AttributeFetchResult getAccessTokenAndProfileOpenIdConnect(OAuthContext context,
			OAuthProviderConfiguration providerCfg) throws Exception
	{
		OIDCProviderMetadata providerMeta = metadataManager.getMetadata(buildMetadataRequest(providerCfg));
		String tokenEndpoint = providerCfg.accessTokenEndpoint;
		if (tokenEndpoint == null)
		{
			if (providerMeta.getTokenEndpointURI() != null)
				tokenEndpoint = providerMeta.getTokenEndpointURI().toString();
			else
				throw new AuthenticationException("The access token endpoint is not set "
						+ "in provider's metadata and it is not configured manually");
		}

		ClientAuthnMode selectedMethod = establishOpenIDAuthnMode(providerMeta, providerCfg);

		HTTPResponse response = retrieveAccessTokenGeneric(context, providerCfg, tokenEndpoint, selectedMethod);
		OIDCTokenResponse acResponse = OIDCTokenResponse.parse(response);
		BearerAccessToken accessToken = extractAccessToken(acResponse);

		JWT idToken = acResponse.getOIDCTokens().getIDToken();
		if (idToken == null)
			throw new AuthenticationException("Id token was not returned by the authorization server");
		JWTClaimsSet accessTokenClaimsSet = idToken.getJWTClaimsSet();
		Map<String, List<String>> accessTokenAttributes = ProfileFetcherUtils.convertToAttributes(
				new JSONObject(accessTokenClaimsSet.getClaims()));

		List<String> userInfoEndpoints = new ArrayList<>(providerCfg.userInfoEndpoints);
		if (userInfoEndpoints.isEmpty() && providerMeta.getUserInfoEndpointURI() != null)
			userInfoEndpoints.add(providerMeta.getUserInfoEndpointURI().toString());

		return fetchUserAttributes(providerCfg, accessToken, accessTokenAttributes, userInfoEndpoints);
	}

	private ClientAuthnMode establishOpenIDAuthnMode(OIDCProviderMetadata providerMeta,
			OAuthProviderConfiguration providerCfg) throws AuthenticationException
	{
		if (ClientAuthnMethod.private_key_jwt == providerCfg.clientAuthnMethod)
			return establishOpenIDAuthnModeForPrivateKeyJwt(providerMeta);
		return establishOpenIDAuthnModeForSecret(providerMeta, providerCfg);
	}

	ClientAuthnMode establishOpenIDAuthnModeForPrivateKeyJwt(OIDCProviderMetadata providerMeta)
			throws AuthenticationException
	{
		List<ClientAuthenticationMethod> supportedMethods = providerMeta.getTokenEndpointAuthMethods();
		if (supportedMethods != null && !supportedMethods.contains(ClientAuthenticationMethod.PRIVATE_KEY_JWT))
			throw new AuthenticationException("Provider does not support private_key_jwt authentication. "
					+ "Supported methods: " + supportedMethods);
		return ClientAuthnMode.secretBasic; // unused — private_key_jwt path ignores ClientAuthnMode
	}

	ClientAuthnMode establishOpenIDAuthnModeForSecret(OIDCProviderMetadata providerMeta,
			OAuthProviderConfiguration providerCfg) throws AuthenticationException
	{
		if (providerCfg.clientAuthnMode.isPresent())
			return providerCfg.clientAuthnMode.get();

		ClientAuthnMode selectedMethod = ClientAuthnMode.secretBasic;
		List<ClientAuthenticationMethod> supportedMethods = providerMeta.getTokenEndpointAuthMethods();
		if (supportedMethods != null)
		{
			selectedMethod = null;
			for (ClientAuthenticationMethod sm : supportedMethods)
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
				throw new AuthenticationException("Client authentication methods supported by"
						+ " the provider (" + supportedMethods + ") do not include "
						+ "any of methods supported by Unity.");
		}
		return selectedMethod;
	}

	private AttributeFetchResult getAccessTokenAndProfilePlain(OAuthContext context,
			OAuthProviderConfiguration providerCfg) throws Exception
	{
		String tokenEndpoint = providerCfg.accessTokenEndpoint;
		ClientAuthnMode selectedMethod = providerCfg.getClientAuthModeFallbackToDefault();
		HTTPResponse response = retrieveAccessTokenGeneric(context, providerCfg, tokenEndpoint, selectedMethod);
		Map<String, List<String>> accessTokenAttributes = new HashMap<>();
		BearerAccessToken accessToken;
		if (providerCfg.accessTokenFormat == AccessTokenFormat.standard)
		{
			JSONObject jsonResp = response.getBodyAsJSONObject();
			if (!jsonResp.containsKey("token_type"))
				jsonResp.put("token_type", AccessTokenType.BEARER.getValue());
			AccessTokenResponse atResponse = AccessTokenResponse.parse(jsonResp);
			accessToken = extractAccessToken(atResponse);
			extractUserInfoFromStandardAccessToken(atResponse, accessTokenAttributes);
		} else
		{
			if (response.getStatusCode() != 200)
				throw new AuthenticationException("Exchange of authorization code for access "
						+ "token failed: " + response.getBody());
			MultiMap<String> map = new MultiMap<>();
			UrlEncoded.decodeTo(Optional.ofNullable(response.getBody())
					.map(r -> r.trim())
					.orElse(""), map, java.nio.charset.StandardCharsets.UTF_8);
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

		List<String> userInfoEndpoints = new ArrayList<>(providerCfg.userInfoEndpoints);
		return fetchUserAttributes(providerCfg, accessToken, accessTokenAttributes, userInfoEndpoints);
	}

	private AttributeFetchResult fetchUserAttributes(OAuthProviderConfiguration providerCfg,
			BearerAccessToken accessToken, Map<String, List<String>> baseAttributes,
			List<String> userInfoEndpoints) throws Exception
	{
		ProfileFetcherConfig fetcherConfig = ProfileFetcherConfig.from(providerCfg);
		AttributeFetchResult fetchRet = new AttributeFetchResult();
		for (String userInfoEndpoint: userInfoEndpoints)
		{
			AttributeFetchResult fetchSingle = providerCfg.userAttributesResolver.fetchProfile(accessToken,
					userInfoEndpoint, fetcherConfig, baseAttributes);
			fetchRet = fetchRet.mergeWith(fetchSingle);
		}
		fetchRet.getAttributes().putAll(baseAttributes);
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

	private OIDCMetadataRequest buildMetadataRequest(OAuthProviderConfiguration providerCfg)
	{
		return OIDCMetadataRequest.builder()
				.withUrl(providerCfg.openIdDiscoveryEndpoint)
				.withValidator(providerCfg.validator)
				.withValidatorName(providerCfg.truststoreName)
				.withHostnameChecking(providerCfg.hostNameCheckingMode)
				.build();
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
		for (OAuthProviderConfiguration provider : getProviders().getAll())
		{
			String idpKey = provider.key.asString();
			if (provider.openIdConnect)
			{
				extractIdPInfoFromOIDCProvider(idpKey, provider).ifPresent(providers::add);
			} else
			{
				IdPInfo providerInfo = IdPInfo.builder()
						.withId(provider.accessTokenEndpoint)
						.withConfigId(idpKey)
						.withDisplayedName(provider.name)
						.withGroup(buildFederationGroup(provider))
						.build();
				providers.add(providerInfo);
			}
		}
		return providers;
	}

	private IdpGroup buildFederationGroup(OAuthProviderConfiguration provider)
	{
		if (provider.federationId == null)
			return null;
		return new IdpGroup(provider.federationId, Optional.ofNullable(provider.federationName));
	}

	private Optional<IdPInfo> extractIdPInfoFromOIDCProvider(String idpKey, OAuthProviderConfiguration provider)
	{
		OIDCProviderMetadata metadata;
		try
		{
			metadata = metadataManager.getMetadata(buildMetadataRequest(provider));
		} catch (Exception e)
		{
			log.warn("Can't obtain OIDC metadata", e);
			return Optional.empty();
		}

		return Optional.of(IdPInfo.builder()
				.withId(metadata.getTokenEndpointURI().toString())
				.withConfigId(idpKey)
				.withDisplayedName(provider.name)
				.withGroup(buildFederationGroup(provider))
				.build());
	}

	@Override
	public AuthenticationMethod getAuthenticationMethod()
	{
		return AuthenticationMethod.U_OAUTH;
	}

	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<OAuth2Verificator> factory,
				SharedEndpointManagement sharedEndpointManagement,
				OAuthContextsManagement contextManagement,
				SharedRemoteAuthenticationContextStore remoteAuthnContextStore,
				OAuthFederationMetadataManager federationManager) throws EngineException
		{
			super(NAME, DESC, factory);

			ServletHolder responseConsumerServlet = new ServletHolder(
					new ResponseConsumerServlet(contextManagement, remoteAuthnContextStore));
			sharedEndpointManagement.deployInternalEndpointServlet(
					ResponseConsumerServlet.PATH, responseConsumerServlet, false);

			ServletHolder federationServlet = new ServletHolder(
					new OAuthFederationEntityStatementServlet(federationManager));
			sharedEndpointManagement.deployInternalEndpointServlet(
					OAuthFederationEntityStatementServlet.PATH, federationServlet, false);
		}
	}
}
