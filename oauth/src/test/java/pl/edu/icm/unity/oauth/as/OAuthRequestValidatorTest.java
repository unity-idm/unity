package pl.edu.icm.unity.oauth.as;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.oauth2.sdk.Scope;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;

class OAuthRequestValidatorTest
{
	private OAuthASProperties oauthConfig;
	private EntityManagement identitiesMan;
	private AttributesManagement attributesMan;
	private OAuthScopesService scopeService;
	private OAuthRequestValidator validator;

	@BeforeEach
	void setUp()
	{
		oauthConfig = mock(OAuthASProperties.class);
		identitiesMan = mock(EntityManagement.class);
		attributesMan = mock(AttributesManagement.class);
		scopeService = mock(OAuthScopesService.class);
		validator = new OAuthRequestValidator(oauthConfig, identitiesMan, attributesMan, scopeService);
	}

	@Test
	void shouldReturnAllWhenScopesAllowedAndDefined()
	{
		ActiveOAuthScopeDefinition scope1 = mockScope("scope1", false);
		ActiveOAuthScopeDefinition scope2 = mockScope("scope2", false);
		when(scopeService.getActiveScopes(any())).thenReturn(List.of(scope1, scope2));

		Map<String, AttributeExt> clientAttributes = new HashMap<>();
		AttributeExt allowedScopes = mock(AttributeExt.class);
		when(allowedScopes.getValues()).thenReturn(List.of("scope1", "scope2"));
		clientAttributes.put(OAuthSystemAttributesProvider.ALLOWED_SCOPES, allowedScopes);

		Scope requestedScopes = new Scope("scope1", "scope2");

		List<RequestedOAuthScope> result = validator.getValidRequestedScopes(clientAttributes, requestedScopes);
		assertEquals(2, result.size());
		assertTrue(result.stream()
				.anyMatch(s -> s.scope()
						.equals("scope1")));
		assertTrue(result.stream()
				.anyMatch(s -> s.scope()
						.equals("scope2")));
	}

	@Test
	void shouldNotReturnNotAllowedScopes()
	{
		ActiveOAuthScopeDefinition scope1 = mockScope("scope1", false);
		ActiveOAuthScopeDefinition scope2 = mockScope("scope2", false);
		when(scopeService.getActiveScopes(any())).thenReturn(List.of(scope1, scope2));

		Map<String, AttributeExt> clientAttributes = new HashMap<>();
		AttributeExt allowedScopes = mock(AttributeExt.class);
		when(allowedScopes.getValues()).thenReturn(List.of("scope1"));
		clientAttributes.put(OAuthSystemAttributesProvider.ALLOWED_SCOPES, allowedScopes);

		Scope requestedScopes = new Scope("scope1", "scope2");

		List<RequestedOAuthScope> result = validator.getValidRequestedScopes(clientAttributes, requestedScopes);
		assertEquals(1, result.size());
		assertEquals("scope1", result.get(0)
				.scope());
	}
	
	@Test
	void shouldNotReturnNotAllowedPatternScopes()
	{
		ActiveOAuthScopeDefinition scope1 = mockScope("scope1.*", true);
		ActiveOAuthScopeDefinition scope2 = mockScope("scope2", false);
		when(scopeService.getActiveScopes(any())).thenReturn(List.of(scope1, scope2));

		Map<String, AttributeExt> clientAttributes = new HashMap<>();
		AttributeExt allowedScopes = mock(AttributeExt.class);
		when(allowedScopes.getValues()).thenReturn(List.of("scope2"));
		clientAttributes.put(OAuthSystemAttributesProvider.ALLOWED_SCOPES, allowedScopes);

		Scope requestedScopes = new Scope("scope1test", "scope2");

		List<RequestedOAuthScope> result = validator.getValidRequestedScopes(clientAttributes, requestedScopes);
		assertEquals(1, result.size());
		assertEquals("scope2", result.get(0)
				.scope());
	}
	
	@Test
	void shouldReturnAllowedSubPatternScopes()
	{
		ActiveOAuthScopeDefinition scope1 = mockScope("scope1.*", true);
		ActiveOAuthScopeDefinition scope2 = mockScope("scope2", false);
		when(scopeService.getActiveScopes(any())).thenReturn(List.of(scope1, scope2));

		Map<String, AttributeExt> clientAttributes = new HashMap<>();
		AttributeExt allowedScopes = mock(AttributeExt.class);
		when(allowedScopes.getValues()).thenReturn(List.of("scope1.*"));
		clientAttributes.put(OAuthSystemAttributesProvider.ALLOWED_SCOPES, allowedScopes);

		AttributeExt canReceivePattern = mock(AttributeExt.class);
		when(canReceivePattern.getValues()).thenReturn(List.of("true"));
		clientAttributes.put(OAuthSystemAttributesProvider.CAN_RECEIVE_PATTERN_SCOPES, canReceivePattern);

		
		Scope requestedScopes = new Scope("scope1test.*", "scope2");

		List<RequestedOAuthScope> result = validator.getValidRequestedScopes(clientAttributes, requestedScopes);
		assertEquals(1, result.size());
		assertEquals("scope1test.*", result.get(0)
				.scope());
	}
	
	@Test
	void shouldReturnAllowedPatternScopes()
	{
		ActiveOAuthScopeDefinition scope1 = mockScope("scope1.*", true);
		ActiveOAuthScopeDefinition scope2 = mockScope("scope2", false);
		when(scopeService.getActiveScopes(any())).thenReturn(List.of(scope1, scope2));

		Map<String, AttributeExt> clientAttributes = new HashMap<>();
		AttributeExt allowedScopes = mock(AttributeExt.class);
		when(allowedScopes.getValues()).thenReturn(List.of("scope1.*"));
		clientAttributes.put(OAuthSystemAttributesProvider.ALLOWED_SCOPES, allowedScopes);

		Scope requestedScopes = new Scope("scope1test", "scope2");

		List<RequestedOAuthScope> result = validator.getValidRequestedScopes(clientAttributes, requestedScopes);
		assertEquals(1, result.size());
		assertEquals("scope1test", result.get(0)
				.scope());
	}

	@Test
	void shouldNotReturnScopeNotDefinedOnServer()
	{
		ActiveOAuthScopeDefinition scope1 = mockScope("scope1", false);
		when(scopeService.getActiveScopes(any())).thenReturn(List.of(scope1));

		Map<String, AttributeExt> clientAttributes = new HashMap<>();
		AttributeExt allowedScopes = mock(AttributeExt.class);
		when(allowedScopes.getValues()).thenReturn(List.of("scope1", "scope2"));
		clientAttributes.put(OAuthSystemAttributesProvider.ALLOWED_SCOPES, allowedScopes);

		Scope requestedScopes = new Scope("scope1", "scope2");

		List<RequestedOAuthScope> result = validator.getValidRequestedScopes(clientAttributes, requestedScopes);
		assertEquals(1, result.size());
		assertEquals("scope1", result.get(0)
				.scope());
	}

	
	
	private ActiveOAuthScopeDefinition mockScope(String name, boolean pattern)
	{
		
		return new ActiveOAuthScopeDefinition(name, name, null, pattern);
	}
}