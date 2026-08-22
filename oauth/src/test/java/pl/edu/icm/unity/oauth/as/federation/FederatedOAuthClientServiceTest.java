/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.federation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.files.RemoteFileData;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.federation.FederatedOAuthClientService.FederatedClientResolution;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;

class FederatedOAuthClientServiceTest
{
	private static final String CLIENT_ID = "https://client.example.com";
	private static final String TRUST_ANCHOR_ID = "https://anchor.example.com";
	private static final String CLIENTS_GROUP = "/oauth-clients";
	private static final String FEDERATION_TRUSTSTORE = "FED_TS";
	private static final long ENTITY_ID = 42L;
	private static final String DISPLAYED_NAME_ATTR = "cn";

	private EntityManagement identitiesMan;
	private AttributesManagement attributesMan;
	private GroupsManagement groupsMan;
	private AttributeSupport attributeSupport;
	private AttributeTypeSupport attrTypeSupport;
	private URIAccessService uriAccessService;
	private ExecutorsService executorsService;
	private UnityServerConfiguration conf;
	private org.springframework.core.env.Environment environment;
	private FederatedOAuthClientService service;
	private OAuthASFederationConfig federationConfig;
	private ECKey signingKey;

	@BeforeEach
	void setUp() throws Exception
	{
		identitiesMan = mock(EntityManagement.class);
		attributesMan = mock(AttributesManagement.class);
		groupsMan = mock(GroupsManagement.class);
		attributeSupport = mock(AttributeSupport.class);
		attrTypeSupport = mock(AttributeTypeSupport.class);
		uriAccessService = mock(URIAccessService.class);
		executorsService = mock(ExecutorsService.class);
		when(executorsService.getExecutionService()).thenReturn(java.util.concurrent.Executors.newSingleThreadExecutor());
		conf = mock(UnityServerConfiguration.class);
		when(conf.getIntValue(UnityServerConfiguration.BULK_FILES_CONNECTION_TIMEOUT)).thenReturn(5000);
		when(conf.getIntValue(UnityServerConfiguration.BULK_FILES_DOWNLOAD_TIMEOUT)).thenReturn(10000);
		when(conf.getIntValue(UnityServerConfiguration.BULK_FILES_MAX_SIZE)).thenReturn(2_097_152);
		environment = mock(org.springframework.core.env.Environment.class);
		when(environment.acceptsProfiles(org.mockito.ArgumentMatchers.any(org.springframework.core.env.Profiles.class)))
				.thenReturn(true);
		signingKey = new ECKeyGenerator(Curve.P_256).keyID("test").generate();

		AttributeType logoType = new AttributeType(OAuthSystemAttributesProvider.CLIENT_LOGO,
				pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax.ID);
		when(attrTypeSupport.getType(OAuthSystemAttributesProvider.CLIENT_LOGO)).thenReturn(logoType);
		doReturn(new ImageAttributeSyntax()).when(attrTypeSupport).getSyntax(logoType);

		service = spy(new FederatedOAuthClientService(identitiesMan, attributesMan, groupsMan, attributeSupport,
				attrTypeSupport, uriAccessService, executorsService, conf, environment));

		federationConfig = new OAuthASFederationConfig(true, TRUST_ANCHOR_ID,
				new JWKSet(signingKey.toPublicJWK()), null, FEDERATION_TRUSTSTORE, null, CLIENTS_GROUP,
				new OAuthFederationClientDefaults(true, List.of()));

		AttributeType cnType = new AttributeType(DISPLAYED_NAME_ATTR,
				pl.edu.icm.unity.stdext.attr.StringAttributeSyntax.ID);
		when(attributeSupport.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME))
				.thenReturn(cnType);
	}

	@Test
	void shouldRegisterNewClient_andReturnEntityId() throws Exception
	{
		OIDCClientMetadata rpMeta = buildRpMetadata();
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());

		Identity identity = mock(Identity.class);
		when(identity.getEntityId()).thenReturn(ENTITY_ID);
		when(identitiesMan.addEntity(any(), any(EntityState.class))).thenReturn(identity);
		when(identitiesMan.getEntity(any())).thenThrow(new IllegalArgumentException("not found"));
		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of());
		when(attributesMan.getAllAttributes(any(), eq(true), eq("/"), any(), eq(false)))
				.thenReturn(List.of());

		FederatedClientResolution result = service.resolveAndRegister(CLIENT_ID, federationConfig);

		assertThat(result.entityId()).isEqualTo(ENTITY_ID);
		verify(identitiesMan).addEntity(any(), eq(EntityState.valid));
		verify(groupsMan).addMemberFromParent(eq(CLIENTS_GROUP), any());
	}

	@Test
	void shouldSetOAuthAttributes_onRegistration() throws Exception
	{
		OIDCClientMetadata rpMeta = buildRpMetadata();
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());

		Identity identity = mock(Identity.class);
		when(identity.getEntityId()).thenReturn(ENTITY_ID);
		when(identitiesMan.addEntity(any(), any(EntityState.class))).thenReturn(identity);
		when(identitiesMan.getEntity(any())).thenThrow(new IllegalArgumentException("not found"));
		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of());
		when(attributesMan.getAllAttributes(any(), eq(true), eq("/"), any(), eq(false)))
				.thenReturn(List.of());

		service.resolveAndRegister(CLIENT_ID, federationConfig);

		verify(attributesMan, times(1)).setAttribute(
				eq(new EntityParam(ENTITY_ID)),
				argThat((Attribute a) -> a.getName().equals(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI)));
	}

	@Test
	void shouldSetDisplayedName_onRegistration() throws Exception
	{
		OIDCClientMetadata rpMeta = buildRpMetadata();
		rpMeta.setName("My RP");
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());

		Identity identity = mock(Identity.class);
		when(identity.getEntityId()).thenReturn(ENTITY_ID);
		when(identitiesMan.addEntity(any(), any(EntityState.class))).thenReturn(identity);
		when(identitiesMan.getEntity(any())).thenThrow(new IllegalArgumentException("not found"));
		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of());
		when(attributesMan.getAllAttributes(any(), eq(true), eq("/"), any(), eq(false)))
				.thenReturn(List.of());

		service.resolveAndRegister(CLIENT_ID, federationConfig);

		verify(attributesMan).setAttribute(
				eq(new EntityParam(ENTITY_ID)),
				argThat(a -> a.getName().equals(DISPLAYED_NAME_ATTR)
						&& a.getGroupPath().equals("/")
						&& a.getValues().get(0).equals("[Federated] My RP")));
	}

	@Test
	void shouldUseFallbackClientId_whenNoClientName() throws Exception
	{
		OIDCClientMetadata rpMeta = buildRpMetadata();
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());

		Identity identity = mock(Identity.class);
		when(identity.getEntityId()).thenReturn(ENTITY_ID);
		when(identitiesMan.addEntity(any(), any(EntityState.class))).thenReturn(identity);
		when(identitiesMan.getEntity(any())).thenThrow(new IllegalArgumentException("not found"));
		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of());
		when(attributesMan.getAllAttributes(any(), eq(true), eq("/"), any(), eq(false)))
				.thenReturn(List.of());

		service.resolveAndRegister(CLIENT_ID, federationConfig);

		verify(attributesMan).setAttribute(
				eq(new EntityParam(ENTITY_ID)),
				argThat(a -> a.getName().equals(DISPLAYED_NAME_ATTR)
						&& a.getValues().get(0).equals("[Federated] " + CLIENT_ID)));
	}

	@Test
	void shouldUseChainCache_andSkipChainResolution() throws Exception
	{
		OIDCClientMetadata rpMeta = buildRpMetadata();
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());

		Identity identity = mock(Identity.class);
		when(identity.getEntityId()).thenReturn(ENTITY_ID);
		when(identitiesMan.addEntity(any(), any(EntityState.class))).thenReturn(identity);

		Entity existingEntity = mock(Entity.class);
		when(existingEntity.getId()).thenReturn(ENTITY_ID);
		when(identitiesMan.getEntity(any()))
				.thenThrow(new IllegalArgumentException("not found"))
				.thenReturn(existingEntity);

		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of());
		when(attributesMan.getAllAttributes(any(), eq(true), eq("/"), any(), eq(false)))
				.thenReturn(List.of());

		service.resolveAndRegister(CLIENT_ID, federationConfig);
		service.resolveAndRegister(CLIENT_ID, federationConfig);

		verify(service, times(1)).resolveAndCacheChain(eq(CLIENT_ID), any());
		verify(identitiesMan, times(1)).addEntity(any(), any(EntityState.class));
	}

	@Test
	void shouldFindEntityInDB_andNotRegister() throws Exception
	{
		OIDCClientMetadata rpMeta = buildRpMetadata();
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());

		Entity existingEntity = mock(Entity.class);
		when(existingEntity.getId()).thenReturn(ENTITY_ID);
		when(identitiesMan.getEntity(any())).thenReturn(existingEntity);
		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of());
		when(attributesMan.getAllAttributes(any(), eq(true), eq("/"), any(), eq(false)))
				.thenReturn(List.of());

		FederatedClientResolution result = service.resolveAndRegister(CLIENT_ID, federationConfig);

		assertThat(result.entityId()).isEqualTo(ENTITY_ID);
		verify(identitiesMan, never()).addEntity(any(), any(EntityState.class));
		verify(groupsMan, never()).addMemberFromParent(any(), any());
	}

	@Test
	void shouldRefreshOAuthAttributes_whenChanged() throws Exception
	{
		OIDCClientMetadata rpMeta = buildRpMetadata();
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());

		Entity existingEntity = mock(Entity.class);
		when(existingEntity.getId()).thenReturn(ENTITY_ID);
		when(identitiesMan.getEntity(any())).thenReturn(existingEntity);
		when(attributesMan.getAllAttributes(any(), eq(true), eq("/"), any(), eq(false)))
				.thenReturn(List.of());

		AttributeExt staleUri = mockAttributeExt(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI,
				CLIENTS_GROUP, List.of("https://old.example.com/cb"));
		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of(staleUri));

		service.resolveAndRegister(CLIENT_ID, federationConfig);

		verify(attributesMan).setAttribute(
				eq(new EntityParam(ENTITY_ID)),
				argThat(a -> a.getName().equals(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI)));
	}

	@Test
	void shouldNotRefreshOAuthAttributes_whenUnchanged() throws Exception
	{
		OIDCClientMetadata rpMeta = buildRpMetadata();
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());

		Entity existingEntity = mock(Entity.class);
		when(existingEntity.getId()).thenReturn(ENTITY_ID);
		when(identitiesMan.getEntity(any())).thenReturn(existingEntity);
		when(attributesMan.getAllAttributes(any(), eq(true), eq("/"), any(), eq(false)))
				.thenReturn(List.of());

		AttributeExt currentUri = mockAttributeExt(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI,
				CLIENTS_GROUP, List.of("https://client.example.com/callback"));
		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of(currentUri));

		service.resolveAndRegister(CLIENT_ID, federationConfig);

		verify(attributesMan, never()).setAttribute(
				any(),
				argThat(a -> a.getName().equals(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI)));
	}

	@Test
	void shouldSetDisplayedName_onRefresh() throws Exception
	{
		OIDCClientMetadata rpMeta = buildRpMetadata();
		rpMeta.setName("New Name");
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());

		Entity existingEntity = mock(Entity.class);
		when(existingEntity.getId()).thenReturn(ENTITY_ID);
		when(identitiesMan.getEntity(any())).thenReturn(existingEntity);
		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of());

		service.resolveAndRegister(CLIENT_ID, federationConfig);

		verify(attributesMan).setAttribute(
				eq(new EntityParam(ENTITY_ID)),
				argThat((Attribute a) -> a.getName().equals(DISPLAYED_NAME_ATTR)
						&& a.getValues().get(0).equals("[Federated] New Name")));
	}

	@Test
	void shouldFetchLogoSynchronously_onFirstRegistration() throws Exception
	{
		URI logoUri = new URI("https://client.example.com/logo.png");
		OIDCClientMetadata rpMeta = buildRpMetadata();
		rpMeta.setLogoURI(logoUri);
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());
		stubLogoDownload(logoUri);

		Identity identity = mock(Identity.class);
		when(identity.getEntityId()).thenReturn(ENTITY_ID);
		when(identitiesMan.addEntity(any(), any(EntityState.class))).thenReturn(identity);
		when(identitiesMan.getEntity(any())).thenThrow(new IllegalArgumentException("not found"));
		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of());
		when(attributesMan.getAllAttributes(any(), eq(true), eq("/"), any(), eq(false)))
				.thenReturn(List.of());

		service.resolveAndRegister(CLIENT_ID, federationConfig);

		// no waiting/timeout - the logo must already be set by the time resolveAndRegister returns,
		// so that it's shown on the very first login of a newly seen federation client
		verify(attributesMan).setAttribute(
				eq(new EntityParam(ENTITY_ID)),
				argThat(a -> a.getName().equals(OAuthSystemAttributesProvider.CLIENT_LOGO)));
	}

	@Test
	void shouldFetchLogoAsynchronously_onRefresh_whenChanged() throws Exception
	{
		URI newLogoUri = new URI("https://client.example.com/new-logo.png");
		OIDCClientMetadata rpMeta = buildRpMetadata();
		rpMeta.setLogoURI(newLogoUri);
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());
		stubLogoDownload(newLogoUri);

		Entity existingEntity = mock(Entity.class);
		when(existingEntity.getId()).thenReturn(ENTITY_ID);
		when(identitiesMan.getEntity(any())).thenReturn(existingEntity);
		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of());
		when(attributesMan.getAllAttributes(any(), eq(true), eq("/"), any(), eq(false)))
				.thenReturn(List.of());

		service.resolveAndRegister(CLIENT_ID, federationConfig);

		// runs in background - allow it to complete rather than asserting it happened inline
		verify(attributesMan, timeout(2000)).setAttribute(
				eq(new EntityParam(ENTITY_ID)),
				argThat(a -> a.getName().equals(OAuthSystemAttributesProvider.CLIENT_LOGO)));
	}

	@Test
	void shouldNotFetchLogoFromInternalDestinationWhenNotInTestProfile() throws Exception
	{
		when(environment.acceptsProfiles(org.mockito.ArgumentMatchers.any(org.springframework.core.env.Profiles.class)))
				.thenReturn(false);
		FederatedOAuthClientService prodService = spy(new FederatedOAuthClientService(identitiesMan, attributesMan, groupsMan,
				attributeSupport, attrTypeSupport, uriAccessService, executorsService, conf, environment));

		URI logoUri = new URI("http://127.0.0.1/logo.png");
		OIDCClientMetadata rpMeta = buildRpMetadata();
		rpMeta.setLogoURI(logoUri);
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(prodService).resolveAndCacheChain(eq(CLIENT_ID), any());

		Identity identity = mock(Identity.class);
		when(identity.getEntityId()).thenReturn(ENTITY_ID);
		when(identitiesMan.addEntity(any(), any(EntityState.class))).thenReturn(identity);
		when(identitiesMan.getEntity(any())).thenThrow(new IllegalArgumentException("not found"));
		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of());
		when(attributesMan.getAllAttributes(any(), eq(true), eq("/"), any(), eq(false)))
				.thenReturn(List.of());

		prodService.resolveAndRegister(CLIENT_ID, federationConfig);

		verify(uriAccessService, never()).readURL(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), anyLong());
		verify(attributesMan, never()).setAttribute(any(),
				argThat(a -> a.getName().equals(OAuthSystemAttributesProvider.CLIENT_LOGO)));
	}

	@Test
	void shouldFetchLatestLogoWhenNewerRequestArrivesWhileFetchInProgress() throws Exception
	{
		URI uriA = new URI("https://client.example.com/logoA.png");
		URI uriB = new URI("https://client.example.com/logoB.png");
		byte[] tinyPng = java.util.Base64.getDecoder().decode(
				"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M8AAAMBAQDJ/AP4AAAAAElFTkSuQmCC");

		CountDownLatch releaseA = new CountDownLatch(1);
		when(uriAccessService.readURL(eq(uriA), eq(FEDERATION_TRUSTSTORE), any(), any(), eq(0), anyLong()))
				.thenAnswer(invocation ->
				{
					releaseA.await(5, TimeUnit.SECONDS);
					return new RemoteFileData(uriA.toString(), tinyPng, new Date(), "image/png");
				});
		when(uriAccessService.readURL(eq(uriB), eq(FEDERATION_TRUSTSTORE), any(), any(), eq(0), anyLong()))
				.thenReturn(new RemoteFileData(uriB.toString(), tinyPng, new Date(), "image/png"));

		Method updateLogoIfChangedAsync = FederatedOAuthClientService.class.getDeclaredMethod(
				"updateLogoIfChangedAsync", String.class, long.class, String.class, URI.class, String.class);
		updateLogoIfChangedAsync.setAccessible(true);

		updateLogoIfChangedAsync.invoke(service, CLIENT_ID, ENTITY_ID, CLIENTS_GROUP, uriA, FEDERATION_TRUSTSTORE);
		updateLogoIfChangedAsync.invoke(service, CLIENT_ID, ENTITY_ID, CLIENTS_GROUP, uriB, FEDERATION_TRUSTSTORE);

		releaseA.countDown();

		// B must actually be fetched and applied after A finishes, not discarded as a duplicate request
		verify(uriAccessService, timeout(2000)).readURL(eq(uriB), eq(FEDERATION_TRUSTSTORE), any(), any(), eq(0), anyLong());
		verify(attributesMan, timeout(2000).times(2)).setAttribute(
				eq(new EntityParam(ENTITY_ID)),
				argThat(a -> a.getName().equals(OAuthSystemAttributesProvider.CLIENT_LOGO)));
	}

	private void stubLogoDownload(URI logoUri) throws Exception
	{
		byte[] tinyPng = java.util.Base64.getDecoder().decode(
				"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M8AAAMBAQDJ/AP4AAAAAElFTkSuQmCC");
		// must use the federation's configured truststore, not the default/system one
		when(uriAccessService.readURL(eq(logoUri), eq(FEDERATION_TRUSTSTORE), any(), any(), eq(0), anyLong()))
				.thenReturn(new pl.edu.icm.unity.engine.api.files.RemoteFileData(logoUri.toString(), tinyPng,
						new java.util.Date(), "image/png"));
	}

	@Test
	void shouldReturnFalse_isKnownFederationClient_initially()
	{
		assertThat(service.isKnownFederationClient(CLIENT_ID)).isFalse();
	}

	@Test
	void shouldReturnTrue_isKnownFederationClient_afterRegistration() throws Exception
	{
		OIDCClientMetadata rpMeta = buildRpMetadata();
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());

		Identity identity = mock(Identity.class);
		when(identity.getEntityId()).thenReturn(ENTITY_ID);
		when(identitiesMan.addEntity(any(), any(EntityState.class))).thenReturn(identity);
		when(identitiesMan.getEntity(any())).thenThrow(new IllegalArgumentException("not found"));
		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of());
		when(attributesMan.getAllAttributes(any(), eq(true), eq("/"), any(), eq(false)))
				.thenReturn(List.of());

		service.resolveAndRegister(CLIENT_ID, federationConfig);

		assertThat(service.isKnownFederationClient(CLIENT_ID)).isTrue();
	}

	@Test
	void shouldInvalidateChainCache_andResolveAgain() throws Exception
	{
		OIDCClientMetadata rpMeta = buildRpMetadata();
		TrustChain chain = buildChain(CLIENT_ID, rpMeta);
		doReturn(chain).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());

		Identity identity = mock(Identity.class);
		when(identity.getEntityId()).thenReturn(ENTITY_ID);
		when(identitiesMan.addEntity(any(), any(EntityState.class))).thenReturn(identity);
		when(identitiesMan.getEntity(any())).thenThrow(new IllegalArgumentException("not found"));
		when(attributesMan.getAllAttributes(any(), eq(true), eq(CLIENTS_GROUP), any(), eq(false)))
				.thenReturn(List.of());
		when(attributesMan.getAllAttributes(any(), eq(true), eq("/"), any(), eq(false)))
				.thenReturn(List.of());

		service.resolveAndRegister(CLIENT_ID, federationConfig);
		service.invalidateChainCache();
		service.resolveAndRegister(CLIENT_ID, federationConfig);

		verify(service, times(2)).resolveAndCacheChain(eq(CLIENT_ID), any());
	}

	@Test
	void shouldThrow_whenChainResolutionFails() throws Exception
	{
		doReturn(null).when(service).resolveAndCacheChain(eq(CLIENT_ID), any());

		assertThatThrownBy(() -> service.resolveAndRegister(CLIENT_ID, federationConfig))
				.isInstanceOf(Exception.class);
	}

	// --- helpers ---

	private OIDCClientMetadata buildRpMetadata() throws Exception
	{
		OIDCClientMetadata meta = new OIDCClientMetadata();
		meta.setRedirectionURIs(Set.of(new URI("https://client.example.com/callback")));
		meta.setScope(Scope.parse("openid profile"));
		meta.setTokenEndpointAuthMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
		meta.setJWKSet(new JWKSet(new RSAKeyGenerator(2048).keyID("client-key").generate().toPublicJWK()));
		return meta;
	}

	private TrustChain buildChain(String clientId, OIDCClientMetadata rpMeta) throws Exception
	{
		Date now = new Date();
		Date exp = Date.from(Instant.now().plusSeconds(3600));
		JWKSet jwkSet = new JWKSet(signingKey.toPublicJWK());

		EntityID leafId = new EntityID(clientId);
		EntityStatementClaimsSet leafClaims = new EntityStatementClaimsSet(leafId, leafId, now, exp, jwkSet);
		leafClaims.setRPInformation(new OIDCClientInformation(new ClientID(clientId), rpMeta));
		EntityStatement leafStatement = EntityStatement.sign(leafClaims, signingKey);

		EntityID anchorId = new EntityID(TRUST_ANCHOR_ID);
		EntityStatementClaimsSet anchorClaims = new EntityStatementClaimsSet(anchorId, leafId, now, exp, jwkSet);
		EntityStatement anchorStatement = EntityStatement.sign(anchorClaims, signingKey);

		return new TrustChain(leafStatement, List.of(anchorStatement));
	}

	private AttributeExt mockAttributeExt(String name, String group, List<String> values)
	{
		AttributeExt attr = mock(AttributeExt.class);
		when(attr.getName()).thenReturn(name);
		when(attr.getGroupPath()).thenReturn(group);
		when(attr.getValues()).thenReturn((List<String>) values);
		return attr;
	}
}
