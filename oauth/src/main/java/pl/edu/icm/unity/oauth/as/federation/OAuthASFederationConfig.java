/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.federation;

import com.nimbusds.jose.jwk.JWKSet;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.token.JwksParseUtils;

public record OAuthASFederationConfig(
		boolean membershipEnabled,
		String trustAnchorId,
		JWKSet trustAnchorJwks,
		X509CertChainValidator validator,
		ServerHostnameCheckingMode hostnameCheckingMode,
		String clientsGroup)
{
	public static OAuthASFederationConfig from(OAuthASProperties props, PKIManagement pkiManagement)
	{
		String trustAnchorId = props.getValue(OAuthASProperties.FEDERATION_TRUST_ANCHOR_ID);
		String jwksStr = props.getValue(OAuthASProperties.FEDERATION_TRUST_ANCHOR_JWKS);
		JWKSet trustAnchorJwks = null;
		if (jwksStr != null && !jwksStr.isBlank())
			trustAnchorJwks = JwksParseUtils.parseRequired(jwksStr, "Invalid federation trust anchor JWKS in AS config");
		String truststoreName = props.getValue(OAuthASProperties.FEDERATION_TRUSTSTORE);
		X509CertChainValidator validator = null;
		if (truststoreName != null && !truststoreName.isBlank())
		{
			try
			{
				if (pkiManagement.getValidatorNames().contains(truststoreName))
					validator = pkiManagement.getValidator(truststoreName);
			} catch (Exception e)
			{
				throw new InternalException(
						"Cannot resolve federation truststore '" + truststoreName + "': " + e.getMessage());
			}
		}
		ServerHostnameCheckingMode hostnameChecking = props.getEnumValue(
				OAuthASProperties.FEDERATION_HOSTNAME_CHECKING, ServerHostnameCheckingMode.class);
		boolean membershipEnabled = props.getBooleanValue(OAuthASProperties.FEDERATION_MEMBERSHIP_ENABLED);
		String clientsGroup = props.getValue(OAuthASProperties.CLIENTS_GROUP);
		return new OAuthASFederationConfig(membershipEnabled, trustAnchorId, trustAnchorJwks, validator,
				hostnameChecking, clientsGroup);
	}
}
