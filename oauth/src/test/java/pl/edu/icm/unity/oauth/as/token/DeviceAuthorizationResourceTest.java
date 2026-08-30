/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.oauth.as.DeviceCodeToken;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.SystemOAuthScopeProvidersRegistry;
import pl.edu.icm.unity.oauth.as.token.access.DeviceCodeRepository;

public class DeviceAuthorizationResourceTest
{
	private static final String DEVICE_SIGNIN_URL = "https://localhost:233/device_signin";

	private OAuthASProperties config;
	private EntityManagement identitiesMan;
	private AttributesManagement attributesMan;
	private DeviceCodeRepository deviceCodeRepository;
	private DeviceAuthorizationResource tested;

	@BeforeEach
	void setUp()
	{
		config = OAuthTestUtils.getConfig();
		config.setProperty(OAuthASProperties.DEVICE_GRANT_ENABLED, "true");

		identitiesMan = mock(EntityManagement.class);
		attributesMan = mock(AttributesManagement.class);
		OAuthScopesService scopeService = new OAuthScopesService(mock(SystemOAuthScopeProvidersRegistry.class));
		OAuthRequestValidator requestValidator = new OAuthRequestValidator(config, identitiesMan, attributesMan,
				scopeService);

		OAuthEndpointsCoordinator coordinator = new OAuthEndpointsCoordinator();
		coordinator.registerDeviceSignInEndpoint(OAuthTestUtils.ISSUER, DEVICE_SIGNIN_URL);

		deviceCodeRepository = new DeviceCodeRepository(new MockTokensMan());

		tested = new DeviceAuthorizationResource(config, coordinator, requestValidator, identitiesMan,
				deviceCodeRepository);
	}

	@Test
	public void shouldFailWhenDeviceGrantDisabled()
	{
		config.setProperty(OAuthASProperties.DEVICE_GRANT_ENABLED, "false");
		setupUnauthenticated();

		Response resp = tested.deviceAuthorization(null, null);

		assertEquals(400, resp.getStatus());
		assertEquals("invalid_request", getError(resp));
	}

	@Test
	public void shouldFailForUnknownClient() throws Exception
	{
		setupUnauthenticated();
		when(identitiesMan.getEntity(any())).thenThrow(new IllegalArgumentException("unknown"));

		Response resp = tested.deviceAuthorization("unknownClient", null);

		assertEquals(401, resp.getStatus());
		assertEquals("invalid_client", getError(resp));
	}

	@Test
	public void shouldFailWhenFlowNotAllowedForClient()
	{
		setupUnauthenticated();
		setupClient(42, "device1", GrantFlow.authorizationCode);

		Response resp = tested.deviceAuthorization("device1", null);

		assertEquals(401, resp.getStatus());
		assertEquals("invalid_client", getError(resp));
	}

	@Test
	public void shouldSucceedForPublicClient() throws Exception
	{
		setupUnauthenticated();
		setupClient(42, "device1", GrantFlow.deviceCode);

		Response resp = tested.deviceAuthorization("device1", null);

		assertEquals(200, resp.getStatus());
		JSONObject body = (JSONObject) JSONValue.parse(resp.getEntity().toString());
		String userCode = (String) body.get("user_code");
		String deviceCode = (String) body.get("device_code");
		assertThat(userCode).isNotBlank();
		assertThat(deviceCode).isNotBlank();
		assertThat((String) body.get("verification_uri")).isEqualTo(DEVICE_SIGNIN_URL);
		assertThat((String) body.get("verification_uri_complete")).contains(DEVICE_SIGNIN_URL)
				.contains(userCode);
		assertThat(((Number) body.get("expires_in")).intValue())
				.isEqualTo(OAuthASProperties.DEFAULT_DEVICE_CODE_VALIDITY);
	}

	@Test
	public void shouldSucceedForConfidentialClientWithSession() throws Exception
	{
		setupInvocationContext(42);
		setupClient(42, "device1", GrantFlow.deviceCode);

		Response resp = tested.deviceAuthorization(null, null);

		assertEquals(200, resp.getStatus());
	}

	@Test
	public void shouldRejectConfidentialClientWithoutSession() throws Exception
	{
		setupUnauthenticated();
		setupClient(42, "device1", "CONFIDENTIAL", GrantFlow.deviceCode);

		Response resp = tested.deviceAuthorization("device1", null);

		assertEquals(401, resp.getStatus());
		assertEquals("invalid_client", getError(resp));
	}

	@Test
	public void shouldNotRequireAuthenticatedSessionForPublicClient() throws Exception
	{
		// regression test: a genuinely anonymous caller (no InvocationContext login session at
		// all, matching how the REST layer handles the optionally-authenticated
		// /device_authorization path when no credentials are sent) must still be able to resolve
		// a public client's identity/attributes. Historically this required entity/attribute
		// lookups guarded by AuthzCapability.read, which throw (or NPE via isSelf()) once there is
		// no login session - the fix routes these lookups through Spring's "insecure"-qualified
		// EntityManagement/AttributesManagement beans instead.
		setupUnauthenticated();
		assertThat(InvocationContext.getCurrent().getLoginSession()).isNull();
		setupClient(42, "device1", "PUBLIC", GrantFlow.deviceCode);

		Response resp = tested.deviceAuthorization("device1", null);

		assertEquals(200, resp.getStatus());
	}

	@Test
	public void shouldRecordRequestedScopeOnStoredToken() throws Exception
	{
		setupUnauthenticated();
		setupClient(42, "device1", GrantFlow.deviceCode);

		Response resp = tested.deviceAuthorization("device1", "sc1 sc2");

		assertEquals(200, resp.getStatus());
		JSONObject body = (JSONObject) JSONValue.parse(resp.getEntity().toString());
		String deviceCode = (String) body.get("device_code");

		OAuthToken storedToken = deviceCodeRepository.getByDeviceCode(deviceCode)
				.map(t -> DeviceCodeToken.getInstanceFromJson(t.getContents()))
				.map(DeviceCodeToken::getOauthToken)
				.orElseThrow();
		// requested scope must be recorded even though the server doesn't define "sc1"/"sc2" -
		// otherwise a later refresh-token grant without an explicit scope NPEs (RefreshTokenHandler
		// falls back to replaying the originally requested scope)
		assertThat(storedToken.getRequestedScope()).containsExactlyInAnyOrder("sc1", "sc2");
	}

	private void setupClient(long entityId, String username, GrantFlow... allowedFlows)
	{
		setupClient(entityId, username, "PUBLIC", allowedFlows);
	}

	private void setupClient(long entityId, String username, String clientType, GrantFlow... allowedFlows)
	{
		Entity entity = mock(Entity.class);
		when(entity.getId()).thenReturn(entityId);
		try
		{
			when(identitiesMan.getEntity(any())).thenReturn(entity);
			when(identitiesMan.getGroups(any())).thenReturn(Map.of(config.getValue(OAuthASProperties.CLIENTS_GROUP),
					mock(pl.edu.icm.unity.base.group.GroupMembership.class)));
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}

		AttributeExt allowedFlowsAttr = mock(AttributeExt.class);
		when(allowedFlowsAttr.getName()).thenReturn(OAuthSystemAttributesProvider.ALLOWED_FLOWS);
		when(allowedFlowsAttr.getValues())
				.thenReturn(List.of(allowedFlows).stream().map(GrantFlow::toString).toList());

		AttributeExt clientTypeAttr = mock(AttributeExt.class);
		when(clientTypeAttr.getName()).thenReturn(OAuthSystemAttributesProvider.CLIENT_TYPE);
		when(clientTypeAttr.getValues()).thenReturn(List.of(clientType));

		try
		{
			when(attributesMan.getAllAttributes(any(), anyBoolean(), anyString(), any(), anyBoolean()))
					.thenReturn(List.of(allowedFlowsAttr, clientTypeAttr));
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private void setupUnauthenticated()
	{
		AuthenticationRealm realm = new AuthenticationRealm("foo", "", 5, 10, RememberMePolicy.disallow, 1, 1000);
		InvocationContext notAuthed = new InvocationContext(null, realm, Collections.emptyList());
		InvocationContext.setCurrent(notAuthed);
	}

	private void setupInvocationContext(long entityId)
	{
		AuthenticationRealm realm = new AuthenticationRealm("foo", "", 5, 10, RememberMePolicy.disallow, 1, 1000);
		InvocationContext ctx = new InvocationContext(null, realm, Collections.emptyList());
		LoginSession loginSession = new LoginSession("sid", new Date(), 1000, entityId, "foo", null, null, null);
		loginSession.addAuthenticatedIdentities(List.of("device1"));
		ctx.setLoginSession(loginSession);
		InvocationContext.setCurrent(ctx);
	}

	private static Object getError(Response resp)
	{
		JSONObject body = (JSONObject) JSONValue.parse(resp.getEntity().toString());
		return body.get("error");
	}
}
