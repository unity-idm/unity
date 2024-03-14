/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import io.imunity.idp.AccessProtocol;
import io.imunity.idp.ApplicationId;
import io.imunity.idp.IdPClientData;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement.LastIdPClientAccessKey;
import io.imunity.vaadin.auth.services.idp.GroupWithIndentIndicator;
import io.imunity.vaadin.auth.services.idp.IdpUsersHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.SigningAlgorithms;
import pl.edu.icm.unity.oauth.as.console.OAuthServiceConfiguration;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.as.token.access.OAuthRefreshTokenRepository;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrustedOAuthClientsManagementTest
{
	@Mock
	private SecuredTokensManagement tokenMan;
	@Mock
	private PreferencesManagement preferencesManagement;
	@Mock
	private OAuthAccessTokenRepository oauthTokenDAO;
	@Mock
	private OAuthRefreshTokenRepository refreshTokenDAO;
	@Mock
	private EndpointManagement endpointManagement;
	private AttributeTypeSupport aTypeSupport;
	@Mock
	private BulkGroupQueryService bulkService;
	@Mock
	private IdpUsersHelper idpUsersHelper;
	@Mock
	private MessageSource msg;
	@Mock
	private OAuthScopesService scopesService;

	@Mock
	private LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement;

	private TrustedOAuthClientsManagement appMan;
	
	@BeforeEach
	public void init()
	{
		 appMan = new TrustedOAuthClientsManagement(tokenMan, preferencesManagement,
				oauthTokenDAO, refreshTokenDAO, endpointManagement, bulkService, idpUsersHelper, msg, scopesService, aTypeSupport,
				lastAccessAttributeManagement);
	}
	
	@Test
	public void shouldGetTrustedApplication() throws EngineException, JsonProcessingException
	{
		setupInvocationContext();

		Instant grantTime = setupPreferences(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		setupTokens(OAuthAccessTokenRepository.INTERNAL_ACCESS_TOKEN);
		setupEndpoints();
		setupClientGroup();
		Instant accessTime = setupAccessTime();

		List<IdPClientData> idpClientsData = appMan.getIdpClientsData();
		assertThat(idpClientsData.size()).isEqualTo(1);
		IdPClientData clientData = idpClientsData.get(0);
		assertThat(clientData.applicationId.id).isEqualTo("clientEntityId");
		assertThat(clientData.accessGrantTime.get()).isEqualTo(grantTime);
		assertThat(clientData.lastAccessTime.get()).isEqualTo(accessTime);
		assertThat(clientData.applicationDomain.get()).isEqualTo("localhost");
		assertThat(clientData.technicalInformations.size()).isEqualTo(1);
		assertThat(clientData.technicalInformations.get(0).value.contains("ac")).isEqualTo(true);

	}

	@Test
	public void shouldGetTrustedApplicationWithoutTokens() throws EngineException, JsonProcessingException
	{
		setupInvocationContext();

		Instant grantTime = setupPreferences(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		setupEndpoints();
		setupClientGroup();
		Instant accessTime = setupAccessTime();

		List<IdPClientData> idpClientsData = appMan.getIdpClientsData();
		assertThat(idpClientsData.size()).isEqualTo(1);
		IdPClientData clientData = idpClientsData.get(0);
		assertThat(clientData.applicationId.id).isEqualTo("clientEntityId");
		assertThat(clientData.accessGrantTime.get()).isEqualTo(grantTime);
		assertThat(clientData.lastAccessTime.get()).isEqualTo(accessTime);
		assertThat(clientData.technicalInformations.size()).isEqualTo(0);
		assertThat(clientData.applicationDomain.get()).isEqualTo("localhost");
	}

	@Test
	public void shouldGetTrustedApplicationWithPreferencesWithoutTimestamp()
			throws EngineException, JsonProcessingException
	{
		
		setupInvocationContext();

		setupPreferences(null);

		setupTokens(OAuthRefreshTokenRepository.INTERNAL_REFRESH_TOKEN);
		setupEndpoints();
		setupClientGroup();
		Instant accessTime = setupAccessTime();

		List<IdPClientData> idpClientsData = appMan.getIdpClientsData();
		assertThat(idpClientsData.size()).isEqualTo(1);
		IdPClientData clientData = idpClientsData.get(0);
		assertThat(clientData.applicationId.id).isEqualTo("clientEntityId");
		assertThat(clientData.lastAccessTime.get()).isEqualTo(accessTime);
		assertThat(clientData.applicationDomain.get()).isEqualTo("localhost");
		assertThat(clientData.technicalInformations.size()).isEqualTo(1);
		assertThat(clientData.technicalInformations.get(0).value.contains("ref")).isEqualTo(true);
	}
	
	@Test
	public void shouldRevokeAccess() throws JsonProcessingException, EngineException
	{
		setupInvocationContext();
		setupTokens(OAuthAccessTokenRepository.INTERNAL_ACCESS_TOKEN);
		setupPreferences(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		appMan.revokeAccess(new ApplicationId("clientEntityId"));
		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(preferencesManagement).setPreference(any(), eq(OAuthPreferences.ID), argument.capture());
		OAuthPreferences pref = new OAuthPreferences();
		pref.setSerializedConfiguration(JsonUtil.parse(argument.getValue()));
		assertThat(pref.getSPSettings("clientEntityId").isDoNotAsk()).isEqualTo(false);
		verify(tokenMan).removeToken(eq(OAuthAccessTokenRepository.INTERNAL_ACCESS_TOKEN), eq("ac"));
	}

	@Test
	public void shouldClearPreferencesWhenUnblockAccess() throws JsonProcessingException, EngineException
	{
		setupInvocationContext();
		setupPreferences(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		appMan.unblockAccess(new ApplicationId("clientEntityId"));
		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(preferencesManagement).setPreference(any(), eq(OAuthPreferences.ID), argument.capture());
		OAuthPreferences pref = new OAuthPreferences();
		pref.setSerializedConfiguration(JsonUtil.parse(argument.getValue()));
		assertThat(pref.getSPSettings("clientEntityId").isDoNotAsk()).isEqualTo(false);
	}

	private Instant setupAccessTime() throws EngineException
	{
		Instant accessTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		when(lastAccessAttributeManagement.getLastAccessByClient())
				.thenReturn(Map.of(new LastIdPClientAccessKey(AccessProtocol.OAuth, "clientEntityId"), accessTime));
		return accessTime;
	}

	private void setupClientGroup() throws EngineException
	{
		Entity mockEntity = mock(Entity.class);
		when(mockEntity.getIdentities())
				.thenReturn(List.of(new Identity(UsernameIdentity.ID, "clientEntityId", 0, "clientEntityId")));
		EntityInGroupData entity1 = new EntityInGroupData(mockEntity, null, Set.of("/"),
				Map.of(OAuthSystemAttributesProvider.ALLOWED_FLOWS,
						new AttributeExt(new Attribute("x", null, null, Collections.emptyList()), false),
						OAuthSystemAttributesProvider.ALLOWED_RETURN_URI,
						new AttributeExt(new Attribute("x", null, null, List.of("https://localhost")), false)

				), null, null);

		GroupMembershipData data1 = new MockGroupMembershipData();
		when(bulkService.getBulkMembershipData(eq("/"))).thenReturn(data1);
		when(bulkService.getMembershipInfo(eq(data1))).thenReturn(ImmutableMap.of(0l, entity1));

	}

	private void setupEndpoints() throws AuthorizationException
	{

		Endpoint endpoint = mock(Endpoint.class);
		when(endpoint.getTypeId()).thenReturn(OAuthAuthzWebEndpoint.NAME);
		EndpointConfiguration config = mock(EndpointConfiguration.class);
		when(endpoint.getConfiguration()).thenReturn(config);
		when(config.getConfiguration()).thenReturn(getIdpProperties());
		when(endpointManagement.getEndpoints()).thenReturn(List.of(endpoint));
	}

	private void setupTokens(String type) throws JsonProcessingException, EngineException
	{
		OAuthToken oauthToken = new OAuthToken();
		oauthToken.setAccessToken("ac");
		oauthToken.setRefreshToken("ref");
		oauthToken.setClientUsername("clientEntityId");
		oauthToken.setClientId(1);
		oauthToken.setIssuerUri("uri");
		String[] scopes =
		{ "scope" };
		oauthToken.setEffectiveScope(scopes);
		Token token = new Token("", "ac", 1L);
		token.setContents(oauthToken.getSerialized());
		token.setType(type);
		token.setCreated(new Date());
		when(oauthTokenDAO.getOwnedAccessTokens()).thenReturn(List.of(token));

	}

	private Instant setupPreferences(Instant grantTime) throws InternalException, EngineException
	{
		OAuthPreferences pref = new OAuthPreferences();
		OAuthClientSettings settings = new OAuthClientSettings();
		settings.setDoNotAsk(true);
		settings.setDefaultAccept(true);
		settings.setTimestamp(grantTime);
		pref.setSPSettings("clientEntityId", settings);
		when(preferencesManagement.getPreference(any(), eq(OAuthPreferences.ID.toString())))
				.thenReturn(JsonUtil.serialize(pref.getSerializedConfiguration()));
		return grantTime;
	}

	private void setupInvocationContext()
	{
		InvocationContext invContext = new InvocationContext(null, null, null);
		invContext.setLoginSession(new LoginSession("1", null, null, 100, 1L, null, null, null, null));
		InvocationContext.setCurrent(invContext);
	}

	private String getIdpProperties()
	{
		OAuthServiceConfiguration config = new OAuthServiceConfiguration();
		config.setIssuerURI("uri");
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

		return config.toProperties(msg);
	}

	public class MockGroupMembershipData implements GroupMembershipData
	{
	}
}
