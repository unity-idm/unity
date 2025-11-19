package pl.edu.icm.unity.oauth.as.webauthz.externalScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;

@ExtendWith(MockitoExtension.class)
class ExternalAuthorizationScriptRunnerTest
{
	@Mock
	private IdentityTypesRegistry identityTypesRegistry;
	private ExternalAuthorizationScriptRunner runner;

	@BeforeEach
	void setUp()
	{
		runner = new ExternalAuthorizationScriptRunner(identityTypesRegistry);
	}

	@Test
	void shouldReturnDenyOnScriptException()
	{
		OAuthAuthzContext ctx = mock(OAuthAuthzContext.class);
		TranslationResult translationResult = mock(TranslationResult.class);
		OAuthASProperties config = mock(OAuthASProperties.class);
		when(config.getStructuredListKeys(anyString())).thenReturn(Set.of("script1"));
		when(config.getValue(contains("triggeringScope"))).thenReturn("scope.*");
		when(config.getValue(contains("path"))).thenReturn("nonExits.py");
		AuthorizationRequest authRequest = mock(AuthorizationRequest.class);
		when(authRequest.getScope()).thenReturn(new com.nimbusds.oauth2.sdk.Scope("scope1"));
		when(ctx.getRequest()).thenReturn(authRequest);
		when(translationResult.getAttributes()).thenReturn(List.of());
		when(translationResult.getIdentities()).thenReturn(List.of());
		
		ExternalAuthorizationScriptResponse result = runner.runConfiguredExternalAuthnScript(ctx,
				translationResult, config);
		
		assertEquals(ExternalAuthorizationScriptResponse.Status.DENY, result.status());
	}

	@Test
	void shouldReturnProceedWhenNoScriptsConfigured()
	{
		OAuthAuthzContext ctx = mock(OAuthAuthzContext.class);
		TranslationResult translationResult = mock(TranslationResult.class);
		OAuthASProperties config = mock(OAuthASProperties.class);
		when(config.getStructuredListKeys(anyString())).thenReturn(Collections.emptySet());
		
		ExternalAuthorizationScriptResponse result = runner.runConfiguredExternalAuthnScript(ctx,
				translationResult, config);
		
		assertEquals(ExternalAuthorizationScriptResponse.Status.PROCEED, result.status());
	}

	@Test
	void shouldReturnProceedWhenNoScriptDenies()
	{
		OAuthAuthzContext ctx = mock(OAuthAuthzContext.class);
		TranslationResult translationResult = mock(TranslationResult.class);
		OAuthASProperties config = mock(OAuthASProperties.class);
		when(config.getStructuredListKeys(anyString())).thenReturn(Set.of("script1"));
		when(config.getValue(contains("triggeringScope"))).thenReturn("scope.*");
		when(config.getValue(contains("path"))).thenReturn("src/test/resources/authorizationScriptProceed.py");
		AuthorizationRequest authRequest = mock(AuthorizationRequest.class);
		when(authRequest.getScope()).thenReturn(new com.nimbusds.oauth2.sdk.Scope("scope1"));
		when(ctx.getRequest()).thenReturn(authRequest);
		when(translationResult.getAttributes()).thenReturn(List.of());
		when(translationResult.getIdentities()).thenReturn(List.of());
		
		ExternalAuthorizationScriptResponse result = runner.runConfiguredExternalAuthnScript(ctx,
				translationResult, config);
		
		assertEquals(ExternalAuthorizationScriptResponse.Status.PROCEED, result.status());
	}

	@Test
	void shouldReturnDenyIfAnyScriptDenies()
	{
		OAuthAuthzContext ctx = mock(OAuthAuthzContext.class);
		TranslationResult translationResult = mock(TranslationResult.class);
		OAuthASProperties config = mock(OAuthASProperties.class);
		when(config.getStructuredListKeys(anyString())).thenReturn(Set.of("script1"));
		when(config.getValue(contains("triggeringScope"))).thenReturn("scope.*");
		when(config.getValue(contains("path"))).thenReturn("src/test/resources/authorizationScriptDeny.py");
		AuthorizationRequest authRequest = mock(AuthorizationRequest.class);
		when(authRequest.getScope()).thenReturn(new com.nimbusds.oauth2.sdk.Scope("scope1"));
		when(ctx.getRequest()).thenReturn(authRequest);
		when(translationResult.getAttributes()).thenReturn(List.of());
		when(translationResult.getIdentities()).thenReturn(List.of());
		
		ExternalAuthorizationScriptResponse result = runner.runConfiguredExternalAuthnScript(ctx,
				translationResult, config);
		
		assertEquals(ExternalAuthorizationScriptResponse.Status.DENY, result.status());
	}

	@Test
	void shouldReturnClaimsInResponse() throws JsonMappingException, JsonProcessingException
	{
		OAuthAuthzContext ctx = mock(OAuthAuthzContext.class);
		TranslationResult translationResult = mock(TranslationResult.class);
		OAuthASProperties config = mock(OAuthASProperties.class);
		when(config.getStructuredListKeys(anyString())).thenReturn(Set.of("script1"));
		when(config.getValue(contains("triggeringScope"))).thenReturn("scope.*");
		when(config.getValue(contains("path"))).thenReturn("src/test/resources/authorizationScriptProceed.py");
		AuthorizationRequest authRequest = mock(AuthorizationRequest.class);
		when(authRequest.getScope()).thenReturn(new com.nimbusds.oauth2.sdk.Scope("scope1"));
		when(ctx.getRequest()).thenReturn(authRequest);
		when(translationResult.getAttributes()).thenReturn(List.of());
		when(translationResult.getIdentities()).thenReturn(List.of());
		
		ExternalAuthorizationScriptResponse result = runner.runConfiguredExternalAuthnScript(ctx,
				translationResult, config);
		
		assertEquals(ExternalAuthorizationScriptResponse.Status.PROCEED, result.status());
		assertThat(result.claims()).containsExactlyInAnyOrder(Claim.builder()
				.withName("test")
				.withValues(List.of(Constants.MAPPER.readTree("\"test\"")))
				.build(),
				Claim.builder()
						.withName("test2")
						.withValues(List.of(Constants.MAPPER.readTree("1"), Constants.MAPPER.readTree("2")))
						.build());

	}

	@Test
	void shouldRunScriptWithMatchingScope() throws JsonMappingException, JsonProcessingException
	{
		OAuthAuthzContext ctx = mock(OAuthAuthzContext.class);
		TranslationResult translationResult = mock(TranslationResult.class);
		OAuthASProperties config = mock(OAuthASProperties.class);
		when(config.getStructuredListKeys(anyString())).thenReturn(Set.of("script1.", "script2."));
		when(config.getValue(contains("script1.triggeringScope"))).thenReturn("scope.*");
		when(config.getValue(contains("script1.path"))).thenReturn("src/test/resources/authorizationScriptProceed.py");
		when(config.getValue(contains("script2.triggeringScope"))).thenReturn("scope2.*");
		when(config.getValue(contains("script2.path"))).thenReturn("src/test/resources/authorizationScriptDeny.py");
		AuthorizationRequest authRequest = mock(AuthorizationRequest.class);
		when(authRequest.getScope()).thenReturn(new com.nimbusds.oauth2.sdk.Scope("scope1"));
		when(ctx.getRequest()).thenReturn(authRequest);
		when(translationResult.getAttributes()).thenReturn(List.of());
		when(translationResult.getIdentities()).thenReturn(List.of());
		
		ExternalAuthorizationScriptResponse result = runner.runConfiguredExternalAuthnScript(ctx,
				translationResult, config);
		
		assertEquals(ExternalAuthorizationScriptResponse.Status.PROCEED, result.status());
	}

}