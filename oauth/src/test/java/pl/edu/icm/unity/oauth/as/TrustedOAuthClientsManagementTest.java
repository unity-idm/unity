/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;

import io.imunity.idp.AccessProtocol;
import io.imunity.idp.ApplicationId;
import io.imunity.idp.IdPClientData;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement.LastIdPClientAccessKey;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.SigningAlgorithms;
import pl.edu.icm.unity.oauth.as.console.OAuthServiceConfiguration;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.webui.common.groups.GroupWithIndentIndicator;
import pl.edu.icm.unity.webui.console.services.idp.IdpUsersHelper;

@RunWith(MockitoJUnitRunner.class)
public class TrustedOAuthClientsManagementTest
{
	@Mock
	private SecuredTokensManagement tokenMan;
	@Mock
	private PreferencesManagement preferencesManagement;
	@Mock
	private OAuthTokenRepository oauthTokenDAO;
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

	@Test
	public void shouldGetTrustedApplication() throws EngineException, JsonProcessingException
	{
		TrustedOAuthClientsManagement appMan = new TrustedOAuthClientsManagement(tokenMan, preferencesManagement,
				oauthTokenDAO, endpointManagement, bulkService, idpUsersHelper, msg, scopesService, aTypeSupport,
				lastAccessAttributeManagement);
		setupInvocationContext();

		Instant grantTime = setupPreferences();
		setupTokens();
		setupEndpoints();
		setupClientGroup();
		Instant accessTime = setupAccessTime();

		List<IdPClientData> idpClientsData = appMan.getIdpClientsData();
		assertThat(idpClientsData.size(), is(1));
		IdPClientData clientData = idpClientsData.get(0);
		assertThat(clientData.applicationId.id, is("clientEntityId"));
		assertThat(clientData.accessGrantTime.get(), is(grantTime));
		assertThat(clientData.lastAccessTime.get(), is(accessTime));
		assertThat(clientData.applicationDomain.get(), is("localhost"));
		assertThat(clientData.technicalInformations.size(), is(1));
		assertThat(clientData.technicalInformations.get(0).value.contains("ac"), is(true));

	}

	@Test
	public void shouldGetTrustedApplicationWithoutTokens() throws EngineException, JsonProcessingException
	{
		TrustedOAuthClientsManagement appMan = new TrustedOAuthClientsManagement(tokenMan, preferencesManagement,
				oauthTokenDAO, endpointManagement, bulkService, idpUsersHelper, msg, scopesService, aTypeSupport,
				lastAccessAttributeManagement);
		setupInvocationContext();

		Instant grantTime = setupPreferences();
		setupEndpoints();
		setupClientGroup();
		Instant accessTime = setupAccessTime();

		List<IdPClientData> idpClientsData = appMan.getIdpClientsData();
		assertThat(idpClientsData.size(), is(1));
		IdPClientData clientData = idpClientsData.get(0);
		assertThat(clientData.applicationId.id, is("clientEntityId"));
		assertThat(clientData.accessGrantTime.get(), is(grantTime));
		assertThat(clientData.lastAccessTime.get(), is(accessTime));
		assertThat(clientData.technicalInformations.size(), is(0));
		assertThat(clientData.applicationDomain.get(), is("localhost"));
	}

	@Test
	public void shouldRevokeAccess() throws JsonProcessingException, EngineException
	{
		TrustedOAuthClientsManagement appMan = new TrustedOAuthClientsManagement(tokenMan, preferencesManagement,
				oauthTokenDAO, endpointManagement, bulkService, idpUsersHelper, msg, scopesService, aTypeSupport,
				lastAccessAttributeManagement);
		setupInvocationContext();
		setupTokens();
		setupPreferences();
		appMan.revokeAccess(new ApplicationId("clientEntityId"));
		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(preferencesManagement).setPreference(any(), eq(OAuthPreferences.ID), argument.capture());
		OAuthPreferences pref = new OAuthPreferences();
		pref.setSerializedConfiguration(JsonUtil.parse(argument.getValue()));
		assertThat(pref.getSPSettings("clientEntityId").isDoNotAsk(), is(false));
		verify(tokenMan).removeToken(eq(OAuthTokenRepository.INTERNAL_ACCESS_TOKEN), eq("ac"));
	}

	@Test
	public void shouldClearPreferencesWhenUnblockAccess() throws JsonProcessingException, EngineException
	{
		TrustedOAuthClientsManagement appMan = new TrustedOAuthClientsManagement(tokenMan, preferencesManagement,
				oauthTokenDAO, endpointManagement, bulkService, idpUsersHelper, msg, scopesService, aTypeSupport,
				lastAccessAttributeManagement);
		setupInvocationContext();
		setupPreferences();
		appMan.unblockAccess(new ApplicationId("clientEntityId"));
		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(preferencesManagement).setPreference(any(), eq(OAuthPreferences.ID), argument.capture());
		OAuthPreferences pref = new OAuthPreferences();
		pref.setSerializedConfiguration(JsonUtil.parse(argument.getValue()));
		assertThat(pref.getSPSettings("clientEntityId").isDoNotAsk(), is(false));
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

	private void setupTokens() throws JsonProcessingException, EngineException
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
		token.setType(OAuthTokenRepository.INTERNAL_ACCESS_TOKEN);
		token.setCreated(new Date());
	//	token.setExpires(new Date());
		when(oauthTokenDAO.getOwnedAccessTokens()).thenReturn(List.of(token));

	}

	private Instant setupPreferences() throws InternalException, EngineException
	{
		OAuthPreferences pref = new OAuthPreferences();
		OAuthClientSettings settings = new OAuthClientSettings();
		settings.setDoNotAsk(true);
		settings.setDefaultAccept(true);
		Instant grantTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
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
