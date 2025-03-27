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
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.oauth.as.AttributeFilteringSpec;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthScope;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthIdPEngine;

@ExtendWith(MockitoExtension.class)
public class TokenServiceTest
{
	@Mock
	private OAuthRequestValidator requestValidator;
	@Mock
	private OAuthASProperties config;
	@Mock
	private OAuthIdPEngine notAuthorizedOauthIdpEngine;
	@Mock
	private ClientAttributesProvider clientAttributesProvider;

	@Test
	public void shouldRespectNewClaimFilterWhenBuildNewTokenBasedOnOldToken()
			throws JsonProcessingException, EngineException, ParseException
	{
		TokenService tokenService = new TokenService(requestValidator, config, notAuthorizedOauthIdpEngine,
				clientAttributesProvider);

		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setRequestedScope(new String[]
		{ "scope1", "scope2" });
		oAuthToken.setEffectiveScope(new String[]
		{ "scope1", "scope2" });
		oAuthToken.setAttributeValueFilters(List.of(new AttributeFilteringSpec("attr1", Set.of("attr1v1"))));
		oAuthToken.setSubject("subject");

		TranslationResult result = new TranslationResult();
		result.getAttributes()
				.add(new DynamicAttribute(new Attribute("attr1", "string", null, List.of("attr1v1"))));
		result.getAttributes()
				.add(new DynamicAttribute(new Attribute("attr2", "string", null, List.of("attr2v1"))));

		when(requestValidator.getValidRequestedScopes(any(), any())).thenReturn(List.of(OAuthScope.builder()
				.withAttributes(List.of("attr1", "attr2"))
				.withName("scope1")
				.build()));
		when(clientAttributesProvider.getClientAttributes(any())).thenReturn(Map.of());

		when(notAuthorizedOauthIdpEngine.getUserInfoUnsafe(anyLong(), any(), any(), any(), any(), any(), any()))
				.thenReturn(result);
		OAuthToken newTokenBasedOnOldToken = tokenService.prepareNewTokenBasedOnOldToken(oAuthToken,
				"scope1 claim_filter:attr2:attr2v2", List.of("scope1", "scope2"), 0, 0, "client", true, "grant");

		assertThat(newTokenBasedOnOldToken.getAttributeValueFilters()).containsExactlyInAnyOrder(
				new AttributeFilteringSpec("attr1", Set.of("attr1v1")), new AttributeFilteringSpec("attr2", Set.of("attr2v2")));

		UserInfo userInfo = UserInfo.parse(newTokenBasedOnOldToken.getUserInfo());
		assertThat(userInfo.getClaim("attr1")).isEqualTo("attr1v1");
		assertThat(userInfo.getClaim("attr2")).isNull();
	}

	@Test
	public void shouldPreserveClaimFilterWhenBuildNewTokenBasedOnOldToken()
			throws JsonProcessingException, EngineException, ParseException
	{
		TokenService tokenService = new TokenService(requestValidator, config, notAuthorizedOauthIdpEngine,
				clientAttributesProvider);

		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setRequestedScope(new String[]
		{ "scope1" });
		oAuthToken.setEffectiveScope(new String[]
		{ "scope1" });
		oAuthToken.setAttributeValueFilters(List.of(new AttributeFilteringSpec("attr1", Set.of("attr1v1"))));
		oAuthToken.setSubject("subject");

		TranslationResult result = new TranslationResult();
		result.getAttributes()
				.add(new DynamicAttribute(new Attribute("attr1", "string", null, List.of("attr1v1"))));

		when(requestValidator.getValidRequestedScopes(any(), any())).thenReturn(List.of(OAuthScope.builder()
				.withAttributes(List.of("attr1"))
				.withName("scope1")
				.build()));
		when(clientAttributesProvider.getClientAttributes(any())).thenReturn(Map.of());

		when(notAuthorizedOauthIdpEngine.getUserInfoUnsafe(anyLong(), any(), any(), any(), any(), any(), any()))
				.thenReturn(result);
		OAuthToken newTokenBasedOnOldToken = tokenService.prepareNewTokenBasedOnOldToken(oAuthToken, "scope1",
				List.of("scope1"), 0, 0, "client", true, "grant");

		assertThat(newTokenBasedOnOldToken.getAttributeValueFilters())
				.containsExactlyInAnyOrder(new AttributeFilteringSpec("attr1", Set.of("attr1v1")));

		UserInfo userInfo = UserInfo.parse(newTokenBasedOnOldToken.getUserInfo());
		assertThat(userInfo.getClaim("attr1")).isEqualTo("attr1v1");
	}
}
