/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.federation;

import java.util.Date;
import java.util.List;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import eu.emi.security.authn.x509.X509Credential;
import pl.edu.icm.unity.oauth.as.token.CredentialJwkConverter;

class OAuthASFederationEntityStatementGenerator
{
	static EntityStatement generate(String entityId, X509Credential federationCredential,
			String superiorEntityId, long validitySeconds,
			OIDCProviderMetadata providerMetadata) throws Exception
	{
		JWKSet federationJwkSet = new JWKSet(CredentialJwkConverter.buildPublicJWK(federationCredential.getCertificate()));

		EntityID entityID = new EntityID(entityId);
		Date now = new Date();
		Date exp = new Date(now.getTime() + validitySeconds * 1000L);

		EntityStatementClaimsSet claims = new EntityStatementClaimsSet(entityID, entityID, now, exp,
				federationJwkSet);

		if (superiorEntityId != null && !superiorEntityId.isEmpty())
			claims.setAuthorityHints(List.of(new EntityID(superiorEntityId)));

		claims.setMetadata(EntityType.OPENID_PROVIDER, providerMetadata.toJSONObject());

		JWK signingJWK = CredentialJwkConverter.buildPrivateJWK(federationCredential);
		return EntityStatement.sign(claims, signingJWK);
	}
}
