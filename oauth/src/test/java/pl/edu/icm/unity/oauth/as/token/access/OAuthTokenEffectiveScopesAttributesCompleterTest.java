package pl.edu.icm.unity.oauth.as.token.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.oauth.as.ActiveOAuthScopeDefinition;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;

@ExtendWith(MockitoExtension.class)
class OAuthTokenEffectiveScopesAttributesCompleterTest
{
	@Mock
	private OAuthScopesService scopeService;
	@Mock
	private OAuthASProperties config;
	@Mock
	private OAuthToken token;

	private OAuthTokenEffectiveScopesAttributesCompleter tokenScopesCompleter;

	@BeforeEach
	void setUp()
	{
		tokenScopesCompleter = new OAuthTokenEffectiveScopesAttributesCompleter(scopeService);
	}

	@Test
	void shouldFixAttributesWhenNullAndScopeExists()
	{
		ActiveOAuthScopeDefinition activeDef = ActiveOAuthScopeDefinition.builder()
				.withName("scope1")
				.withAttributes(List.of("attr1"))
				.build();
		RequestedOAuthScope scope = new RequestedOAuthScope("scope1", ActiveOAuthScopeDefinition.builder()
				.withName("scope1")
				.withAttributes(null)
				.build(), false);
		when(scopeService.getActiveScopes(config)).thenReturn(List.of(activeDef));
		when(token.getEffectiveScope()).thenReturn(List.of(scope));

		tokenScopesCompleter.fixScopesAttributesIfNeeded(config, token);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<RequestedOAuthScope>> captor = ArgumentCaptor.forClass(List.class);
		verify(token).setEffectiveScope(captor.capture());
		List<RequestedOAuthScope> result = captor.getValue();
		assertEquals(1, result.size());
		assertEquals(List.of("attr1"), result.get(0)
				.scopeDefinition()
				.attributes());
	}

	@Test
	void shouldFixAttributesWhenNullAndScopeNotExists()
	{
		RequestedOAuthScope scope = new RequestedOAuthScope("scope2", ActiveOAuthScopeDefinition.builder()
				.withName("scope2")
				.withAttributes(null)
				.build(), false);
		when(scopeService.getActiveScopes(config)).thenReturn(List.of());
		when(token.getEffectiveScope()).thenReturn(List.of(scope));

		tokenScopesCompleter.fixScopesAttributesIfNeeded(config, token);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<RequestedOAuthScope>> captor = ArgumentCaptor.forClass(List.class);
		verify(token).setEffectiveScope(captor.capture());
		List<RequestedOAuthScope> result = captor.getValue();
		assertEquals(1, result.size());
		assertEquals(List.of(), result.get(0)
				.scopeDefinition()
				.attributes());
	}

	@Test
	void shouldNotFixAttributesWhenNotNull()
	{
		RequestedOAuthScope scope = new RequestedOAuthScope("scope3", ActiveOAuthScopeDefinition.builder()
				.withName("scope3")
				.withAttributes(List.of("attrX"))
				.build(), false);
		when(scopeService.getActiveScopes(config)).thenReturn(List.of());
		when(token.getEffectiveScope()).thenReturn(List.of(scope));

		tokenScopesCompleter.fixScopesAttributesIfNeeded(config, token);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<RequestedOAuthScope>> captor = ArgumentCaptor.forClass(List.class);
		verify(token).setEffectiveScope(captor.capture());
		List<RequestedOAuthScope> result = captor.getValue();
		assertEquals(1, result.size());
		assertEquals(List.of("attrX"), result.get(0)
				.scopeDefinition()
				.attributes());
	}

	@Test
	void shouldHandleEmptyEffectiveScope()
	{
		when(scopeService.getActiveScopes(config)).thenReturn(List.of());
		when(token.getEffectiveScope()).thenReturn(List.of());

		tokenScopesCompleter.fixScopesAttributesIfNeeded(config, token);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<RequestedOAuthScope>> captor = ArgumentCaptor.forClass(List.class);
		verify(token).setEffectiveScope(captor.capture());
		List<RequestedOAuthScope> result = captor.getValue();
		assertTrue(result.isEmpty());
	}
}