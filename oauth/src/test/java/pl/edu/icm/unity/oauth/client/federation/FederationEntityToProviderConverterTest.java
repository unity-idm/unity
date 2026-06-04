/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.openid.connect.sdk.SubjectType;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderConfiguration;
import pl.edu.icm.unity.oauth.client.federation.FederationEntityToProviderConverter.FederationProvider;

public class FederationEntityToProviderConverterTest
{
	private static final EntityID LEAF_ENTITY_ID = new EntityID("https://idp.example.com");
	private static final EntityID TRUST_ANCHOR_ID = new EntityID("https://anchor.example.com");
	private static final String CLIENT_ID = "https://rp.example.com/oauth";
	private static final String CLIENT_CREDENTIAL = "myCred";
	private static final TranslationProfile TRANSLATION_PROFILE =
			TranslationProfileGenerator.generateIncludeInputProfile("sys:oidc");

	private ECKey testKey;
	private FederationEntityToProviderConverter converter;

	@BeforeEach
	void setUp() throws Exception
	{
		testKey = new ECKeyGenerator(Curve.P_256).keyID("test-kid").generate();
		converter = new FederationEntityToProviderConverter();
	}

	@Test
	void shouldConvertChainWithFullOpMetadata() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", "https://userinfo.example.com");
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		List<FederationProvider> result = converter.convert(List.of(chain), CLIENT_ID, CLIENT_CREDENTIAL,
				TRANSLATION_PROFILE, null, true);

		assertThat(result).hasSize(1);
		OAuthProviderConfiguration provider = result.get(0).config();
		assertThat(provider.authorizationEndpoint).isEqualTo("https://auth.example.com");
		assertThat(provider.accessTokenEndpoint).isEqualTo("https://token.example.com");
		assertThat(provider.userInfoEndpoints).containsExactly("https://userinfo.example.com");
	}

	@Test
	void shouldSkipChainWithNullOpMetadata() throws Exception
	{
		TrustChain chain = buildChain(LEAF_ENTITY_ID, null, TRUST_ANCHOR_ID, null);

		List<FederationProvider> result = converter.convert(List.of(chain), CLIENT_ID, CLIENT_CREDENTIAL,
				TRANSLATION_PROFILE, null, true);

		assertThat(result).isEmpty();
	}

	@Test
	void shouldUseIssuerAsProviderName() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://my-idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		OAuthProviderConfiguration provider = singleProvider(chain);

		assertThat(provider.name.getDefaultValue()).isEqualTo("https://my-idp.example.com");
	}

	@Test
	void shouldExtractScopesFromOpMeta() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		opMeta.setScopes(Scope.parse("openid email profile"));
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		OAuthProviderConfiguration provider = singleProvider(chain);

		assertThat(provider.scopes).contains("openid");
		assertThat(provider.scopes).contains("email");
		assertThat(provider.scopes).contains("profile");
	}

	@Test
	void shouldDefaultScopesToOpenidWhenNotPresent() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		OAuthProviderConfiguration provider = singleProvider(chain);

		assertThat(provider.scopes).isEqualTo("openid");
	}

	@Test
	void shouldExtractLogoUriFromCustomOpMetaParameter() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		opMeta.setCustomParameter("logo_uri", "https://idp.example.com/logo.png");
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		OAuthProviderConfiguration provider = singleProvider(chain);

		assertThat(provider.iconUrl).isNotNull();
		assertThat(provider.iconUrl.getDefaultValue()).isEqualTo("https://idp.example.com/logo.png");
	}

	@Test
	void shouldSetNullIconUrlWhenLogoUriNotPresent() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		OAuthProviderConfiguration provider = singleProvider(chain);

		assertThat(provider.iconUrl).isNull();
	}

	@Test
	void shouldSetFederationIdFromTrustAnchorEntityId() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		OAuthProviderConfiguration provider = singleProvider(chain);

		assertThat(provider.federationId).isEqualTo(TRUST_ANCHOR_ID.getValue());
	}

	@Test
	void shouldResolveFederationNameFromTrustAnchorOrganizationName() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		FederationEntityMetadata anchorFedMeta = new FederationEntityMetadata();
		anchorFedMeta.setOrganizationName("My Federation");
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, anchorFedMeta);

		OAuthProviderConfiguration provider = singleProvider(chain);

		assertThat(provider.federationName).isEqualTo("My Federation");
	}

	@Test
	void shouldFallbackFederationNameToIdWhenOrganizationNameAbsent() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		OAuthProviderConfiguration provider = singleProvider(chain);

		assertThat(provider.federationName).isEqualTo(TRUST_ANCHOR_ID.getValue());
	}

	@Test
	void shouldSetClientIdFromParameter() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		OAuthProviderConfiguration provider = singleProvider(chain);

		assertThat(provider.clientId).isEqualTo(CLIENT_ID);
	}

	@Test
	void shouldSetClientCredentialFromParameter() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		OAuthProviderConfiguration provider = singleProvider(chain);

		assertThat(provider.clientCredential).isEqualTo(CLIENT_CREDENTIAL);
	}

	@Test
	void shouldSetTranslationProfileFromParameter() throws Exception
	{
		TranslationProfile profile = TranslationProfileGenerator.generateIncludeInputProfile("myProfile");
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		List<FederationProvider> result = converter.convert(List.of(chain), CLIENT_ID, CLIENT_CREDENTIAL,
				profile, null, true);

		assertThat(result.get(0).config().translationProfile).isSameAs(profile);
	}

	@Test
	void shouldSetRegistrationFormFromParameter() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		List<FederationProvider> result = converter.convert(List.of(chain), CLIENT_ID, CLIENT_CREDENTIAL,
				TRANSLATION_PROFILE, "myRegistrationForm", true);

		assertThat(result.get(0).config().registrationForm).isEqualTo("myRegistrationForm");
	}

	@Test
	void shouldSetEnableAssociationFromParameter() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		List<FederationProvider> enabled = converter.convert(List.of(chain), CLIENT_ID, CLIENT_CREDENTIAL,
				TRANSLATION_PROFILE, null, true);
		List<FederationProvider> disabled = converter.convert(List.of(chain), CLIENT_ID, CLIENT_CREDENTIAL,
				TRANSLATION_PROFILE, null, false);

		assertThat(enabled.get(0).config().enableAssociation).isTrue();
		assertThat(disabled.get(0).config().enableAssociation).isFalse();
	}

	@Test
	void shouldBuildDiscoveryEndpointFromIssuer() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		OAuthProviderConfiguration provider = singleProvider(chain);

		assertThat(provider.openIdDiscoveryEndpoint)
				.isEqualTo("https://idp.example.com/.well-known/openid-configuration");
	}

	@Test
	void shouldSkipInvalidChainsAndConvertValid() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		TrustChain validChain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);
		TrustChain nullMetaChain = buildChain(new EntityID("https://other.example.com"), null, TRUST_ANCHOR_ID, null);

		List<FederationProvider> result = converter.convert(List.of(validChain, nullMetaChain),
				CLIENT_ID, CLIENT_CREDENTIAL, TRANSLATION_PROFILE, null, true);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).config().name.getDefaultValue()).isEqualTo("https://idp.example.com");
	}

	@Test
	void shouldMarkProviderKeyAsFromFederation() throws Exception
	{
		OIDCProviderMetadata opMeta = buildOpMeta("https://idp.example.com",
				"https://auth.example.com", "https://token.example.com", null);
		TrustChain chain = buildChain(LEAF_ENTITY_ID, opMeta, TRUST_ANCHOR_ID, null);

		OAuthProviderConfiguration provider = singleProvider(chain);

		assertThat(provider.key.isFromFederation()).isTrue();
	}

	// --- helpers ---

	private OAuthProviderConfiguration singleProvider(TrustChain chain) throws Exception
	{
		List<FederationProvider> result = converter.convert(List.of(chain), CLIENT_ID, CLIENT_CREDENTIAL,
				TRANSLATION_PROFILE, null, true);
		assertThat(result).hasSize(1);
		return result.get(0).config();
	}

	private TrustChain buildChain(EntityID leafId, OIDCProviderMetadata opMeta,
			EntityID trustAnchorId, FederationEntityMetadata trustAnchorFedMeta) throws Exception
	{
		Date now = new Date();
		Date exp = Date.from(Instant.now().plusSeconds(3600));
		JWKSet jwkSet = new JWKSet(testKey.toPublicJWK());

		// Leaf self-statement: issuer=leaf, subject=leaf
		EntityStatementClaimsSet leafClaims = new EntityStatementClaimsSet(
				leafId, leafId, now, exp, jwkSet);
		if (opMeta != null)
			leafClaims.setOPMetadata(opMeta);
		EntityStatement leafStatement = EntityStatement.sign(leafClaims, testKey);

		// Trust anchor's statement about the leaf: issuer=trustAnchor, subject=leaf
		// This is what TrustChain requires: superior's subject must match leaf's issuer
		EntityStatementClaimsSet anchorAboutLeafClaims = new EntityStatementClaimsSet(
				trustAnchorId, leafId, now, exp, jwkSet);
		if (trustAnchorFedMeta != null)
			anchorAboutLeafClaims.setFederationEntityMetadata(trustAnchorFedMeta);
		EntityStatement anchorAboutLeafStatement = EntityStatement.sign(anchorAboutLeafClaims, testKey);

		return new TrustChain(leafStatement, List.of(anchorAboutLeafStatement));
	}

	private OIDCProviderMetadata buildOpMeta(String issuer, String authEndpoint,
			String tokenEndpoint, String userInfoEndpoint) throws Exception
	{
		OIDCProviderMetadata meta = new OIDCProviderMetadata(
				new com.nimbusds.oauth2.sdk.id.Issuer(issuer),
				List.of(SubjectType.PUBLIC),
				URI.create("https://jwks.example.com"));
		meta.setAuthorizationEndpointURI(URI.create(authEndpoint));
		meta.setTokenEndpointURI(URI.create(tokenEndpoint));
		if (userInfoEndpoint != null)
			meta.setUserInfoEndpointURI(URI.create(userInfoEndpoint));
		return meta;
	}

}
