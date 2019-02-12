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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.ALLOWED_RETURN_URI;
import static pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.CLIENT_TYPE;

import java.net.URI;
import java.util.Properties;

import org.assertj.core.util.Maps;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

public class OAuthWebRequestValidatorTest
{
	@Test
	public void shouldAcceptIpv4LoopbackRedirectWithDifferentPort() throws Exception
	{
		OAuthASProperties oauthConfig = getConfig();
		OAuthWebRequestValidator validator = getValidator(oauthConfig, "http://127.0.0.1/some/path");
		
		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType("code"), new ClientID("client"))
				.redirectionURI(new URI("http://127.0.0.1:1234/some/path"))
				.codeChallenge(new CodeVerifier("************************************************"), S256)
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
				.codeChallenge(new CodeVerifier("************************************************"), S256)
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
				.codeChallenge(new CodeVerifier("************************************************"), S256)
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
				.codeChallenge(new CodeVerifier("************************************************"), S256)
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
				.codeChallenge(new CodeVerifier("************************************************"), S256)
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
				.codeChallenge(new CodeVerifier("************************************************"), S256)
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
				.codeChallenge(new CodeVerifier("************************************************"), S256)
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
				.codeChallenge(new CodeVerifier("************************************************"), S256)
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
				.codeChallenge(new CodeVerifier("************************************************"), S256)
				.build();
		OAuthAuthzContext context = new OAuthAuthzContext(request, oauthConfig);
		
		Throwable error = catchThrowable(() -> validator.validate(context));

		assertThat(error).isNull();
	}
	
	private static OAuthASProperties getConfig()
	{
		Properties config = new Properties();
		config.setProperty("unity.oauth2.as.issuerUri", "http://unity.example.com");
		config.setProperty("unity.oauth2.as.signingAlgorithm", "HS256");
		config.setProperty("unity.oauth2.as.signingSecret", "*************************************************************");
		return new OAuthASProperties(config, null, null);
	}

	private static OAuthWebRequestValidator getValidator(OAuthASProperties oauthConfig,
			String authorizedURI) throws EngineException
	{
		AttributesManagement attributesMan = mock(AttributesManagement.class);
		EntityManagement identitiesMan = mock(EntityManagement.class);
		Entity client = mock(Entity.class);
		when(identitiesMan.getEntity(any())).thenReturn(client);
		EntityParam clientEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "client"));
		when(identitiesMan.getGroups(eq(clientEntity))).thenReturn(Maps.newHashMap("/oauth-clients", null));
		AttributeExt allowedFlows = new AttributeExt(StringAttribute.of(ALLOWED_RETURN_URI, "/oauth-clients", 
				authorizedURI), true);
		AttributeExt clientType = new AttributeExt(StringAttribute.of(CLIENT_TYPE, "/oauth-clients", ClientType.PUBLIC.name()), true);
		when(attributesMan.getAllAttributes(eq(clientEntity), anyBoolean(), any(), any(), anyBoolean()))
			.thenReturn(Lists.newArrayList(allowedFlows, clientType));
		return new OAuthWebRequestValidator(oauthConfig, identitiesMan, attributesMan);
	}
}
