/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console.tokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.imunity.vaadin.auth.services.idp.GroupWithIndentIndicator;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.SigningAlgorithms;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.console.OAuthServiceConfiguration;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.as.token.access.OAuthRefreshTokenRepository;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;

@ExtendWith(MockitoExtension.class)
class OAuthTokenControllerTest
{
	private static final String SERVICE = "oauth-service";
	private static final String ISSUER = "https://localhost:2443/oauth2";

	@Mock
	private MessageSource msg;
	@Mock
	private OAuthAccessTokenRepository accessTokenRepository;
	@Mock
	private OAuthRefreshTokenRepository refreshTokenRepository;
	@Mock
	private SecuredTokensManagement tokensManagement;
	@Mock
	private BulkGroupQueryService bulkService;
	@Mock
	private AttributeSupport attributeSupport;
	@Mock
	private EndpointManagement endpointManagement;

	private OAuthTokenController controller;

	@BeforeEach
	void init()
	{
		controller = new OAuthTokenController(msg, accessTokenRepository, refreshTokenRepository, tokensManagement,
				bulkService, attributeSupport, endpointManagement);
	}

	@Test
	void shouldSkipUnparsableTokenWhenListingServiceTokens()
			throws EngineException, JsonProcessingException, ControllerException
	{
		setupEndpoint();
		byte[] unparsableContents = "{\"effectiveScope\":{\"unexpected\":true}}".getBytes(StandardCharsets.UTF_8);
		Token unparsable = token("broken", unparsableContents);
		Token valid = token("valid", validTokenContents());
		when(accessTokenRepository.getAllAccessTokens()).thenReturn(List.of(unparsable, valid));

		Collection<OAuthTokenBean> tokens = controller.getOAuthTokens(SERVICE);

		assertThat(tokens).extracting(OAuthTokenBean::getId).containsExactly("valid");
	}

	private void setupEndpoint() throws EngineException
	{
		Endpoint endpoint = mock(Endpoint.class);
		when(endpoint.getTypeId()).thenReturn(OAuthAuthzWebEndpoint.Factory.TYPE.getName());
		when(endpoint.getName()).thenReturn(SERVICE);
		EndpointConfiguration config = mock(EndpointConfiguration.class);
		when(endpoint.getConfiguration()).thenReturn(config);
		when(config.getConfiguration()).thenReturn(idpProperties());
		when(endpointManagement.getEndpoints()).thenReturn(List.of(endpoint));
	}

	private static Token token(String value, byte[] contents)
	{
		Token token = new Token(OAuthAccessTokenRepository.INTERNAL_ACCESS_TOKEN, value, 1L);
		token.setContents(contents);
		token.setCreated(new Date());
		return token;
	}

	private static byte[] validTokenContents() throws JsonProcessingException
	{
		OAuthToken oauthToken = new OAuthToken();
		oauthToken.setAccessToken("ac");
		oauthToken.setIssuerUri(ISSUER);
		return oauthToken.getSerialized();
	}

	private String idpProperties()
	{
		OAuthServiceConfiguration config = new OAuthServiceConfiguration();
		config.setIssuerURI(ISSUER);
		config.setAccessTokenFormat(AccessTokenFormat.AS_REQUESTED);
		config.setRefreshTokenIssuePolicy(RefreshTokenIssuePolicy.NEVER);
		config.setIdentityTypeForSubject("");
		config.setTranslationProfile(TranslationProfileGenerator.generateEmbeddedEmptyOutputProfile());
		config.setClientGroup(new GroupWithIndentIndicator(new Group("/"), false));
		config.setUsersGroup(new GroupWithIndentIndicator(new Group("/"), false));
		config.setAccessTokenExpiration(1);
		config.setRefreshTokenExpiration(1);
		config.setIdTokenExpiration(1);
		config.setCodeTokenExpiration(1);
		config.setSigningAlg(SigningAlgorithms.ES256);
		config.setFederationMetadataValidity(1);
		return config.toProperties(msg, null);
	}
}
