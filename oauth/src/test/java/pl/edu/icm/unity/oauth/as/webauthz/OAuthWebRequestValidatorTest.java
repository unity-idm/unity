/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import static com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod.S256;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.ALLOWED_RETURN_URI;
import static pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.ALLOWED_SCOPES;
import static pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.CLIENT_TYPE;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.Prompt;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.oauth.as.SystemOAuthScopeProvidersRegistry;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

public class OAuthWebRequestValidatorTest
{
	@Test
	public void shouldAcceptIpv4LoopbackRedirectWithDifferentPort() throws Exception
	{
		OAuthASProperties oauthConfig = getConfig();
		OAuthWebRequestValidator validator = getValidator(oauthConfig, "http://127.0.0.1/some/path");
		
		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"), new ClientID("client"))
				.redirectionURI(new URI("http://127.0.0.1:1234/some/path"))
				.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
				.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, oauthConfig);
		
		Throwable error = catchThrowable(() -> validator.validate(context));

		assertThat(error).isNull();
	}
	
	@Test
	public void shouldAcceptIpv6LoopbackRedirectWithDifferentPort() throws Exception
	{
		OAuthASProperties oauthConfig = getConfig();
		OAuthWebRequestValidator validator = getValidator(oauthConfig, "http://[::1]/some/path");
		
		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"), new ClientID("client"))
				.redirectionURI(new URI("http://[::1]:1234/some/path"))
				.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
				.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, oauthConfig);
		
		Throwable error = catchThrowable(() -> validator.validate(context));

		assertThat(error).isNull();
	}

	@Test
	public void shouldAcceptIpv4LoopbackRedirectWithDifferentScheme() throws Exception
	{
		OAuthASProperties oauthConfig = getConfig();
		OAuthWebRequestValidator validator = getValidator(oauthConfig, "https://127.0.0.1/some/path");
		
		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"), new ClientID("client"))
				.redirectionURI(new URI("http://127.0.0.1/some/path"))
				.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
				.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, oauthConfig);
		
		Throwable error = catchThrowable(() -> validator.validate(context));

		assertThat(error).isNull();
	}
	
	@Test
	public void shouldAcceptIpv6LoopbackRedirectWithDifferentScheme() throws Exception
	{
		OAuthASProperties oauthConfig = getConfig();
		OAuthWebRequestValidator validator = getValidator(oauthConfig, "https://[::1]:1234/some/path");
		
		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"), new ClientID("client"))
				.redirectionURI(new URI("http://[::1]:1234/some/path"))
				.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
				.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, oauthConfig);
		
		Throwable error = catchThrowable(() -> validator.validate(context));

		assertThat(error).isNull();
	}

	@Test
	public void shouldDenyIpv4LoopbackRedirectWithDifferentPath() throws Exception
	{
		OAuthASProperties oauthConfig = getConfig();
		OAuthWebRequestValidator validator = getValidator(oauthConfig, "http://127.0.0.1/some/path");
		
		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"), new ClientID("client"))
				.redirectionURI(new URI("http://127.0.0.1/OTHER"))
				.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
				.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, oauthConfig);
		
		Throwable error = catchThrowable(() -> validator.validate(context));

		assertThat(error).isInstanceOf(OAuthValidationException.class);
	}

	@Test
	public void shouldDenyIpv6LoopbackRedirectWithDifferentPath() throws Exception
	{
		OAuthASProperties oauthConfig = getConfig();
		OAuthWebRequestValidator validator = getValidator(oauthConfig, "http://[::1]/some/path");
		
		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"), new ClientID("client"))
				.redirectionURI(new URI("http://[::1]/OTHER"))
				.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
				.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, oauthConfig);
		
		Throwable error = catchThrowable(() -> validator.validate(context));

		assertThat(error).isInstanceOf(OAuthValidationException.class);
	}

	@Test
	public void shouldDenyNonLoopbackRedirectWithDifferentPort() throws Exception
	{
		OAuthASProperties oauthConfig = getConfig();
		OAuthWebRequestValidator validator = getValidator(oauthConfig, "http://222.2.2.2:1234");
		
		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"), new ClientID("client"))
				.redirectionURI(new URI("http://222.2.2.2:9999"))
				.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
				.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, oauthConfig);
		
		Throwable error = catchThrowable(() -> validator.validate(context));

		assertThat(error).isInstanceOf(OAuthValidationException.class);
	}
	
	@Test
	public void shouldDenyPrivateUseURIWithoutDot() throws Exception
	{
		OAuthASProperties oauthConfig = getConfig();
		OAuthWebRequestValidator validator = getValidator(oauthConfig, "private:/some/path");
		
		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"), new ClientID("client"))
				.redirectionURI(new URI("private:/some/path"))
				.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
				.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, oauthConfig);
		
		Throwable error = catchThrowable(() -> validator.validate(context));

		assertThat(error).isInstanceOf(OAuthValidationException.class);
	}

	@Test
	public void shouldAllowPrivateUseURIWithDot() throws Exception
	{
		OAuthASProperties oauthConfig = getConfig();
		OAuthWebRequestValidator validator = getValidator(oauthConfig, "private.scheme:/some/path");
		
		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"), new ClientID("client"))
				.redirectionURI(new URI("private.scheme:/some/path"))
				.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
				.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, oauthConfig);
		
		Throwable error = catchThrowable(() -> validator.validate(context));

		assertThat(error).isNull();
	}
	
	@Test
	public void shouldSkipOfflineAccessIfNoConsentPrompt()
			throws EngineException, URISyntaxException, OAuthValidationException, ParseException
	{
		Properties config = new Properties();
		config.setProperty("unity.oauth2.as.scopes.99.name", OIDCScopeValue.OFFLINE_ACCESS.getValue());
		config.setProperty("unity.oauth2.as.issuerUri", "http://unity.example.com");
	
		OAuthASProperties props = new OAuthASProperties(config, null, null);
		OAuthWebRequestValidator validator = getValidator(props, "http://222.2.2.2:9999");

		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"),
				new ClientID("client")).redirectionURI(new URI("http://222.2.2.2:9999"))
						.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
						.scope(Scope.parse(OIDCScopeValue.OFFLINE_ACCESS.getValue()))
						.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, props);

		validator.validate(context);

		assertThat(context.getEffectiveRequestedScopes().stream()
				.filter(s -> s.scope().equals(OIDCScopeValue.OFFLINE_ACCESS.getValue())).findAny().isEmpty())
						.isTrue();

	}
	
	@Test
	public void shouldErrorWhenIdTokenClaimsSetAndOpenidIsNotConfigured()
			throws EngineException, URISyntaxException, OAuthValidationException, ParseException
	{
		Properties config = new Properties();
		config.setProperty("unity.oauth2.as.scopes.99.name", "profile");
		config.setProperty("unity.oauth2.as.issuerUri", "http://unity.example.com");
	
		OAuthASProperties props = new OAuthASProperties(config, null, null);
		OAuthWebRequestValidator validator = getValidator(props, "http://222.2.2.2:9999");

		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"),
				new ClientID("client")).redirectionURI(new URI("http://222.2.2.2:9999"))
						.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
						.scope(Scope.parse(OIDCScopeValue.PROFILE.getValue()))
						.customParameter("claims_in_tokens", "id_token")
						.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, props);
		Throwable error = catchThrowable(() -> validator.validate(context));
		assertThat(error).isNotNull();
	}
	
	@Test
	public void shouldErrorWhenTokenClaimsSetAndJWTIsNotConfigured()
			throws EngineException, URISyntaxException, OAuthValidationException, ParseException
	{
		Properties config = new Properties();
		config.setProperty("unity.oauth2.as.scopes.99.name", "profile");
		config.setProperty("unity.oauth2.as.issuerUri", "http://unity.example.com");
		config.setProperty("unity.oauth2.as.tokenFormat", "PLAIN");
		
		OAuthASProperties props = new OAuthASProperties(config, null, null);
		OAuthWebRequestValidator validator = getValidator(props, "http://222.2.2.2:9999");

		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"),
				new ClientID("client")).redirectionURI(new URI("http://222.2.2.2:9999"))
						.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
						.scope(Scope.parse(OIDCScopeValue.PROFILE.getValue()))
						.customParameter("claims_in_tokens", "token")
						.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, props);
		Throwable error = catchThrowable(() -> validator.validate(context));
		assertThat(error).isNotNull();
	}
	
	
	@Test
	public void shouldTrimScopesToAllowedByIdpAndClient()
			throws EngineException, URISyntaxException, OAuthValidationException, ParseException
	{
		Properties config = new Properties();
		config.setProperty("unity.oauth2.as.scopes.1.name", "Scope1");
		config.setProperty("unity.oauth2.as.scopes.2.name", "ToSkip1");
		config.setProperty("unity.oauth2.as.scopes.3.name", "ToSkip2");
		config.setProperty("unity.oauth2.as.issuerUri", "http://unity.example.com");
		config.setProperty("unity.oauth2.as.refreshTokenIssuePolicy", "NEVER");
		
		OAuthASProperties props = new OAuthASProperties(config, null, null);
		OAuthWebRequestValidator validator = getValidator(props, "http://222.2.2.2:9999",
				Optional.of(Arrays.asList("Scope1")));

		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"),
				new ClientID("client")).redirectionURI(new URI("http://222.2.2.2:9999"))
						.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
						.scope(Scope.parse("Scope1 ToSkip1 ToSkip2 ToSkip3")).build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, props);

		validator.validate(context);

		assertThat(context.getEffectiveRequestedScopes().size()).isEqualTo(1);
		assertThat(context.getEffectiveRequestedScopes().iterator().next().scope()).isEqualTo("Scope1");
	}
	
	@Test
	public void shouldTrimScopesToAllowedByIdp()
			throws EngineException, URISyntaxException, OAuthValidationException, ParseException
	{
		Properties config = new Properties();
		config.setProperty("unity.oauth2.as.scopes.1.name", "Scope1");
		config.setProperty("unity.oauth2.as.scopes.2.name", "Scope2");
		config.setProperty("unity.oauth2.as.scopes.3.name", "ToSkip4");
		config.setProperty("unity.oauth2.as.issuerUri", "http://unity.example.com");
		config.setProperty("unity.oauth2.as.refreshTokenIssuePolicy", "NEVER");
		
		OAuthASProperties props = new OAuthASProperties(config, null, null);
		OAuthWebRequestValidator validator = getValidator(props, "http://222.2.2.2:9999",
				Optional.empty());

		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"),
				new ClientID("client")).redirectionURI(new URI("http://222.2.2.2:9999"))
						.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
						.scope(Scope.parse("Scope1 ToSkip1 ToSkip2 Scope2")).build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, props);

		validator.validate(context);

		assertThat(context.getEffectiveRequestedScopes().size()).isEqualTo(2);
		assertThat(context.getEffectiveRequestedScopes().stream().map(s -> s.scope()).collect(Collectors.toSet())).contains("Scope1", "Scope2");		
	}
	
	@Test
	public void shouldTrimScopesToAllowedByIdpWithWildcard()
			throws EngineException, URISyntaxException, OAuthValidationException, ParseException
	{
		Properties config = new Properties();
		config.setProperty("unity.oauth2.as.scopes.1.name", "scope.*foo.?");
		config.setProperty("unity.oauth2.as.scopes.1.isWildcard", "true");
		config.setProperty("unity.oauth2.as.scopes.2.name", "Scope2");
		config.setProperty("unity.oauth2.as.scopes.3.name", "ToSkip4");
		config.setProperty("unity.oauth2.as.issuerUri", "http://unity.example.com");
		config.setProperty("unity.oauth2.as.refreshTokenIssuePolicy", "NEVER");
		
		OAuthASProperties props = new OAuthASProperties(config, null, null);
		OAuthWebRequestValidator validator = getValidator(props, "http://222.2.2.2:9999",
				Optional.empty());

		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"),
				new ClientID("client")).redirectionURI(new URI("http://222.2.2.2:9999"))
						.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
						.scope(Scope.parse("scopeAAfooB ToSkip1 ToSkip2 Scope2")).build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, props);

		validator.validate(context);

		assertThat(context.getEffectiveRequestedScopes().size()).isEqualTo(2);
		assertThat(context.getEffectiveRequestedScopesList()).contains("scopeAAfooB", "Scope2");
	}
	
	@Test
	public void shouldTrimScopesToAllowedByIdpAsRegularString()
			throws EngineException, URISyntaxException, OAuthValidationException, ParseException
	{
		Properties config = new Properties();
		config.setProperty("unity.oauth2.as.scopes.1.name", "scope.*foo.?");
		config.setProperty("unity.oauth2.as.scopes.1.isWildcard", "false");
		config.setProperty("unity.oauth2.as.scopes.2.name", "Scope2");
		config.setProperty("unity.oauth2.as.scopes.3.name", "ToSkip4");
		config.setProperty("unity.oauth2.as.issuerUri", "http://unity.example.com");
		config.setProperty("unity.oauth2.as.refreshTokenIssuePolicy", "NEVER");
		
		OAuthASProperties props = new OAuthASProperties(config, null, null);
		OAuthWebRequestValidator validator = getValidator(props, "http://222.2.2.2:9999",
				Optional.empty());

		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"),
				new ClientID("client")).redirectionURI(new URI("http://222.2.2.2:9999"))
						.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
						.scope(Scope.parse("scopeAAfooB ToSkip1 ToSkip2 Scope2")).build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, props);

		validator.validate(context);

		assertThat(context.getEffectiveRequestedScopes().size()).isEqualTo(1);
		assertThat(context.getEffectiveRequestedScopesList()).contains("Scope2");
	}
	
	@Test
	public void shouldGetFirstMatchingWildcardScope()
			throws EngineException, URISyntaxException, OAuthValidationException, ParseException
	{
		Properties config = new Properties();
		config.setProperty("unity.oauth2.as.scopes.1.name", "scope.*");
		config.setProperty("unity.oauth2.as.scopes.1.isWildcard", "true");
		config.setProperty("unity.oauth2.as.scopes.1.description", "scope1");
		config.setProperty("unity.oauth2.as.scopes.2.name", "scope?b");
		config.setProperty("unity.oauth2.as.scopes.2.description", "scope2");
		config.setProperty("unity.oauth2.as.scopes.2.isWildcard", "true");
		config.setProperty("unity.oauth2.as.issuerUri", "http://unity.example.com");
		config.setProperty("unity.oauth2.as.refreshTokenIssuePolicy", "NEVER");
		
		OAuthASProperties props = new OAuthASProperties(config, null, null);
		OAuthWebRequestValidator validator = getValidator(props, "http://222.2.2.2:9999",
				Optional.empty());

		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"),
				new ClientID("client")).redirectionURI(new URI("http://222.2.2.2:9999"))
						.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
						.scope(Scope.parse("scope1b")).build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, props);

		validator.validate(context);

		assertThat(context.getEffectiveRequestedScopes().size()).isEqualTo(1);
		assertThat(context.getEffectiveRequestedScopes().stream().map(s -> s.scopeDefinition().description).collect(Collectors.toSet())).contains("scope1");
	}
	
	@Test
	public void shouldProcessOfflineAccessIfConsentPrompt()
			throws EngineException, URISyntaxException, OAuthValidationException, ParseException
	{
		Properties config = new Properties();
		config.setProperty("unity.oauth2.as.scopes.99.name", OIDCScopeValue.OFFLINE_ACCESS.getValue());
		config.setProperty("unity.oauth2.as.issuerUri", "http://unity.example.com");
	
		OAuthASProperties props = new OAuthASProperties(config, null, null);
		OAuthWebRequestValidator validator = getValidator(props, "http://222.2.2.2:9999");

		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"),
				new ClientID("client")).redirectionURI(new URI("http://222.2.2.2:9999"))
						.codeChallenge(new CodeVerifier("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"), S256)
						.prompt(Prompt.parse(Prompt.Type.CONSENT.toString()))
						.scope(Scope.parse(OIDCScopeValue.OFFLINE_ACCESS.getValue()))
						.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, props);

		validator.validate(context);

		assertThat(context.getEffectiveRequestedScopes().stream()
				.filter(s -> s.scope().equals(OIDCScopeValue.OFFLINE_ACCESS.getValue())).findAny().isEmpty())
						.isFalse();
	}
	
	private static OAuthASProperties getConfig()
	{
		Properties config = new Properties();
		config.setProperty("unity.oauth2.as.refreshTokenIssuePolicy", "NEVER");
		config.setProperty("unity.oauth2.as.issuerUri", "http://unity.example.com");
		config.setProperty("unity.oauth2.as.signingAlgorithm", "HS256");
		config.setProperty("unity.oauth2.as.signingSecret", "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		return new OAuthASProperties(config, null, null);
	}
	
	private static OAuthWebRequestValidator getValidator(OAuthASProperties oauthConfig,
			String authorizedURI) throws EngineException
	{
		return getValidator(oauthConfig, authorizedURI, Optional.empty());
	}
	private static OAuthWebRequestValidator getValidator(OAuthASProperties oauthConfig,
			String authorizedURI, Optional<List<String>> allowedScopes) throws EngineException
	{
		AttributesManagement attributesMan = mock(AttributesManagement.class);
		EntityManagement identitiesMan = mock(EntityManagement.class);
		Entity client = mock(Entity.class);
		when(identitiesMan.getEntity(any())).thenReturn(client);
		EntityParam clientEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "client"));
		when(identitiesMan.getGroups(eq(clientEntity))).thenReturn(Maps.newHashMap("/oauth-clients", null));
		AttributeExt allowedFlows = new AttributeExt(StringAttribute.of(ALLOWED_RETURN_URI, "/oauth-clients", 
				authorizedURI), true);
		
		AttributeExt allowedScopesA = null;
		if (!allowedScopes.isEmpty())
		{
			allowedScopesA = new AttributeExt(StringAttribute.of(ALLOWED_SCOPES, "/oauth-clients", allowedScopes.get()),
					true);
		}
		AttributeExt clientType = new AttributeExt(StringAttribute.of(CLIENT_TYPE, "/oauth-clients", ClientType.PUBLIC.name()), true);
		when(attributesMan.getAllAttributes(eq(clientEntity), anyBoolean(), anyString(), any(), anyBoolean()))
			.thenReturn( allowedScopesA == null ? Lists.newArrayList(allowedFlows, clientType) : Lists.newArrayList(allowedFlows, clientType, allowedScopesA));
		
		return new OAuthWebRequestValidator(oauthConfig, identitiesMan, attributesMan, new OAuthScopesService(mock(SystemOAuthScopeProvidersRegistry.class)));
	}
}
