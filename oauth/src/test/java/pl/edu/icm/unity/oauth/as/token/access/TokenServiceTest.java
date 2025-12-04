/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.oauth.as.ActiveOAuthScopeDefinition;
import pl.edu.icm.unity.oauth.as.AttributeFilteringSpec;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthIdPEngine;

@ExtendWith(MockitoExtension.class)
public class TokenServiceTest
{
	@Mock
	private OAuthASProperties config;
	@Mock
	private OAuthIdPEngine notAuthorizedOauthIdpEngine;

	private TokenService newService() {
		return new TokenService(config, notAuthorizedOauthIdpEngine);
	}

	@Test
	public void shouldRespectNewClaimFilterWhenBuildNewTokenBasedOnOldTokenForTokenExchange()
			throws JsonProcessingException, EngineException, ParseException
	{
		OAuthToken oldToken = buildTokenWithTwoScopes();
		TranslationResult tr = buildFullTranslationResult();
		mockUserInfo(tr);

		OAuthToken newToken = newService().prepareNewTokenBasedOnOldTokenForTokenExchange(
				oldToken,
				new Scope("scope1", "claim_filter:attr2:attr2v2"),
				List.of("scope1", "scope2"),
				0, 0, List.of("client"), true, "grant"
		);

		assertTwoAttributeFilters(newToken);
		assertClaimsForTwoFilters(newToken);
	}

	@Test
	public void shouldRespectNewClaimFilterWhenBuildNewTokenBasedOnOldTokenForTokenRefresh()
			throws JsonProcessingException, EngineException, ParseException
	{
		OAuthToken oldToken = buildTokenWithTwoScopes();
		TranslationResult tr = buildFullTranslationResult();
		mockUserInfo(tr);

		OAuthToken newToken = newService().prepareNewTokenBasedOnOldTokenForTokenRefresh(
				oldToken,
				new Scope("scope1", "claim_filter:attr2:attr2v2"),
				List.of("scope1", "scope2"),
				0, 0, List.of("client"), true, "grant"
		);

		assertTwoAttributeFilters(newToken);
		assertClaimsForTwoFilters(newToken);
	}

	@Test
	public void shouldPreserveClaimFilterWhenBuildNewTokenBasedOnOldTokenForTokenExchange()
			throws JsonProcessingException, EngineException, ParseException
	{
		OAuthToken oldToken = buildTokenWithSingleScope();
        TranslationResult tr = buildSingleAttributeTranslationResult();
		mockUserInfo(tr);

		OAuthToken newToken = newService().prepareNewTokenBasedOnOldTokenForTokenExchange(
				oldToken, new Scope("scope1"),
				List.of("scope1"),
				0, 0, List.of("client"), true, "grant"
		);

		assertSingleAttributeFilter(newToken);
		assertSingleClaim(newToken);
	}

	@Test
	public void shouldPreserveClaimFilterWhenBuildNewTokenBasedOnOldTokenForTokenRefresh()
			throws JsonProcessingException, EngineException, ParseException
	{
		OAuthToken oldToken = buildTokenWithSingleScope();
        TranslationResult tr = buildSingleAttributeTranslationResult();
		mockUserInfo(tr);

		OAuthToken newToken = newService().prepareNewTokenBasedOnOldTokenForTokenRefresh(
				oldToken, new Scope("scope1"),
				List.of("scope1"),
				0, 0, List.of("client"), true, "grant"
		);

		assertSingleAttributeFilter(newToken);
		assertSingleClaim(newToken);
	}

	/* ============================================================
	 *                     Helper methods
	 * ============================================================ */

	private OAuthToken buildTokenWithTwoScopes() {
		OAuthToken t = new OAuthToken();
		t.setRequestedScope(new String[]{"scope1", "scope2"});
		t.setEffectiveScope(List.of(
				new RequestedOAuthScope("scope1",
						ActiveOAuthScopeDefinition.builder()
								.withName("scope1")
								.withAttributes(List.of("attr1", "attr2"))
								.build(),
						false),
				new RequestedOAuthScope("scope2",
						ActiveOAuthScopeDefinition.builder()
								.withName("scope2").build(), false)
		));
		t.setAttributeValueFilters(List.of(new AttributeFilteringSpec("attr1", Set.of("attr1v1"))));
		t.setSubject("subject");
		return t;
	}

	private OAuthToken buildTokenWithSingleScope() {
		OAuthToken t = new OAuthToken();
		t.setRequestedScope(new String[]{"scope1"});
		t.setEffectiveScope(List.of(
				new RequestedOAuthScope("scope1",
						ActiveOAuthScopeDefinition.builder()
								.withName("scope1")
								.withAttributes(List.of("attr1"))
								.build(),
						false)
		));
		t.setAttributeValueFilters(List.of(new AttributeFilteringSpec("attr1", Set.of("attr1v1"))));
		t.setSubject("subject");
		return t;
	}

	private TranslationResult buildFullTranslationResult() {
		TranslationResult tr = new TranslationResult();
		tr.getAttributes().add(new DynamicAttribute(
				new Attribute("attr1", "string", null, List.of("attr1v1"))
		));
		tr.getAttributes().add(new DynamicAttribute(
				new Attribute("attr2", "string", null, List.of("attr2v1"))
		));
		return tr;
	}

	private TranslationResult buildSingleAttributeTranslationResult() {
		TranslationResult tr = new TranslationResult();
		tr.getAttributes().add(new DynamicAttribute(
				new Attribute("attr1", "string", null, List.of("attr1v1"))
		));
		return tr;
	}

	private void mockUserInfo(TranslationResult tr) throws EngineException {
		when(notAuthorizedOauthIdpEngine.getUserInfoUnsafe(anyLong(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(tr);
	}


	private void assertTwoAttributeFilters(OAuthToken token) {
		assertThat(token.getAttributeValueFilters())
				.containsExactlyInAnyOrder(
						new AttributeFilteringSpec("attr1", Set.of("attr1v1")),
						new AttributeFilteringSpec("attr2", Set.of("attr2v2"))
				);
	}

	private void assertClaimsForTwoFilters(OAuthToken token) throws ParseException {
		UserInfo info = UserInfo.parse(token.getUserInfo());
		assertThat(info.getClaim("attr1")).isEqualTo("attr1v1");
		assertThat(info.getClaim("attr2")).isNull();
	}

	private void assertSingleAttributeFilter(OAuthToken token) {
		assertThat(token.getAttributeValueFilters())
				.containsExactly(new AttributeFilteringSpec("attr1", Set.of("attr1v1")));
	}

	private void assertSingleClaim(OAuthToken token) throws ParseException {
		UserInfo info = UserInfo.parse(token.getUserInfo());
		assertThat(info.getClaim("attr1")).isEqualTo("attr1v1");
	}
}

