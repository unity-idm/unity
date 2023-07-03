/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.imunity.idp.AccessProtocol;
import io.imunity.idp.ApplicationId;
import io.imunity.idp.IdPClientData;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement.LastIdPClientAccessKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.web.SamlAuthVaadinEndpoint;
import pl.edu.icm.unity.saml.idp.web.SamlIdPWebEndpointFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SAMLTrustedApplicationManagementTest
{
	@Mock
	private PreferencesManagement preferencesManagement;
	@Mock
	private EndpointManagement endpointManagement;
	@Mock
	private MessageSource msg;
	@Mock
	private URIAccessService uriAccessService;
	@Mock
	private LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement;
	@Mock
	private PKIManagement pkiManagement;

	@Test
	public void shouldGetTrustedApplication() throws EngineException
	{

		SAMLTrustedApplicationManagement appMan = new SAMLTrustedApplicationManagement(preferencesManagement,
				endpointManagement, msg, uriAccessService, lastAccessAttributeManagement);
		setupInvocationContext();

		Instant grantTime = setupPreferences();
		setupEndpoint();
		Instant accessTime = setupAccessTime();

		List<IdPClientData> idpClientsData = appMan.getIdpClientsData();

		assertThat(idpClientsData.size(), is(1));
		IdPClientData clientData = idpClientsData.get(0);
		assertThat(clientData.applicationId.id, is("clientEntityId"));
		assertThat(clientData.accessGrantTime.get(), is(grantTime));
		assertThat(clientData.lastAccessTime.get(), is(accessTime));
		assertThat(clientData.applicationDomain.get(), is("URL"));
	}

	@Test
	public void shouldClearPreferencesWhenRevokeAccess() throws JsonProcessingException, EngineException
	{
		SAMLTrustedApplicationManagement appMan = new SAMLTrustedApplicationManagement(preferencesManagement,
				endpointManagement, msg, uriAccessService, lastAccessAttributeManagement);
		setupInvocationContext();
		setupPreferences();
		appMan.revokeAccess(new ApplicationId("clientEntityId"));

		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(preferencesManagement).setPreference(any(), eq(SamlPreferences.ID), argument.capture());
		SamlPreferences pref = new SamlPreferences();
		pref.setSerializedConfiguration(JsonUtil.parse(argument.getValue()));
		assertThat(pref.getSPSettings("clientEntityId").isDoNotAsk(), is(false));

	}

	@Test
	public void shouldClearPreferencesWhenUnblockAccess() throws JsonProcessingException, EngineException
	{
		SAMLTrustedApplicationManagement appMan = new SAMLTrustedApplicationManagement(preferencesManagement,
				endpointManagement, msg, uriAccessService, lastAccessAttributeManagement);
		setupInvocationContext();
		setupPreferences();
		appMan.unblockAccess(new ApplicationId("clientEntityId"));

		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(preferencesManagement).setPreference(any(), eq(SamlPreferences.ID), argument.capture());
		SamlPreferences pref = new SamlPreferences();
		pref.setSerializedConfiguration(JsonUtil.parse(argument.getValue()));
		assertThat(pref.getSPSettings("clientEntityId").isDoNotAsk(), is(false));
	}

	private Instant setupAccessTime() throws EngineException
	{
		Instant accessTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		when(lastAccessAttributeManagement.getLastAccessByClient())
				.thenReturn(Map.of(new LastIdPClientAccessKey(AccessProtocol.SAML, "clientEntityId"), accessTime));
		return accessTime;
	}

	private void setupEndpoint() throws EngineException
	{
		SamlAuthVaadinEndpoint instance = mock(SamlAuthVaadinEndpoint.class);
		ResolvedEndpoint rendpoint = mock(ResolvedEndpoint.class);
		when(instance.getEndpointDescription()).thenReturn(rendpoint);
		Endpoint endpoint = mock(Endpoint.class);
		when(rendpoint.getEndpoint()).thenReturn(endpoint);
		when(endpoint.getTypeId()).thenReturn(SamlIdPWebEndpointFactory.TYPE.getName());
		SAMLIdPConfiguration configuration = getSAMLIdPConfiguration();
		when(instance.getSpsConfiguration()).thenReturn(configuration.trustedServiceProviders);
		when(endpointManagement.getDeployedEndpointInstances()).thenReturn(List.of(instance));

	}

	private Instant setupPreferences() throws InternalException, EngineException
	{
		SamlPreferences pref = new SamlPreferences();
		SPSettings settings = new SPSettings();
		settings.setDoNotAsk(true);
		settings.setDefaultAccept(true);
		Instant grantTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		settings.setTimestamp(grantTime);
		pref.setSPSettings("clientEntityId", settings);
		when(preferencesManagement.getPreference(any(), eq(SamlPreferences.ID.toString())))
				.thenReturn(JsonUtil.serialize(pref.getSerializedConfiguration()));
		return grantTime;
	}

	private SAMLIdPConfiguration getSAMLIdPConfiguration()
	{
		return SAMLIdPConfiguration.builder()
				.withCredentialName("MAIN")
				.withPublishMetadata(false)
				.withIssuerURI("me")
				.withGroupChooser(Map.of("group", "group"),"group")
				.withTrustedServiceProviders(new TrustedServiceProviders(
						List.of(TrustedServiceProvider.builder()
								.withEntityId("clientEntityId")
								.withReturnUrl("URL")
								.withName(new I18nString("Name"))
								.build())
				)).build();
	}

	private void setupInvocationContext()
	{
		InvocationContext invContext = new InvocationContext(null, null, null);
		invContext.setLoginSession(new LoginSession("1", null, null, 100, 1L, null, null, null, null));
		InvocationContext.setCurrent(invContext);
	}

}
