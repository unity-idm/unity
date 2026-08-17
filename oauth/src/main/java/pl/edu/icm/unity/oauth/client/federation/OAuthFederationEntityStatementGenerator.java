/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.registration.ClientRegistrationType;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import eu.emi.security.authn.x509.X509Credential;
import pl.edu.icm.unity.oauth.as.token.CredentialJwkConverter;
import pl.edu.icm.unity.oauth.as.token.KeyIdExtractor;

class OAuthFederationEntityStatementGenerator
{
	static EntityStatement generate(OAuthFederationEntityStatementConfig config) throws Exception
	{
		JWKSet federationJwkSet = new JWKSet(CredentialJwkConverter.buildPublicJWK(config.federationCredential().getCertificate()));

		EntityID entityID = new EntityID(config.entityId());
		Date now = new Date();
		Date exp = new Date(Math.addExact(now.getTime(), Math.multiplyExact(config.validitySeconds(), 1000L)));

		EntityStatementClaimsSet claims = new EntityStatementClaimsSet(entityID, entityID, now, exp, federationJwkSet);

		if (config.superiorEntityId() != null && !config.superiorEntityId()
				.isEmpty())
			claims.setAuthorityHints(List.of(new EntityID(config.superiorEntityId())));

		OIDCClientMetadata rpMetadata = new OIDCClientMetadata();
		rpMetadata.setRedirectionURI(new URI(config.callbackUrl()));
		rpMetadata.setResponseTypes(Set.of(ResponseType.CODE));
		rpMetadata.setGrantTypes(Set.of(GrantType.AUTHORIZATION_CODE));
		rpMetadata.setClientRegistrationTypes(List.of(ClientRegistrationType.AUTOMATIC));
		rpMetadata.setJWKSet(buildRpJwkSet(config));
		if (config.organizationName() != null && !config.organizationName().isBlank())
			rpMetadata.setOrganizationName(config.organizationName());
		if (config.logoUri() != null && !config.logoUri().isBlank())
			rpMetadata.setLogoURI(new URI(config.logoUri()));

		OIDCClientInformation rpInfo = new OIDCClientInformation(new ClientID(config.entityId()), rpMetadata);
		claims.setRPInformation(rpInfo);

		JWK signingJWK = CredentialJwkConverter.buildPrivateJWK(config.federationCredential());
		return EntityStatement.sign(claims, signingJWK);
	}

	private static JWKSet buildRpJwkSet(OAuthFederationEntityStatementConfig config) throws JOSEException
	{
		X509Credential authCred = config.authenticationCredential();
		if (authCred == null)
			return new JWKSet(CredentialJwkConverter.buildPublicJWK(config.federationCredential().getCertificate()));

		String authKid = KeyIdExtractor.getKeyId(authCred.getCertificate());
		String fedKid = KeyIdExtractor.getKeyId(config.federationCredential().getCertificate());
		if (authKid.equals(fedKid))
			return new JWKSet(CredentialJwkConverter.buildPublicJWK(config.federationCredential().getCertificate()));

		return new JWKSet(CredentialJwkConverter.buildPublicJWK(authCred.getCertificate()));
	}
}
