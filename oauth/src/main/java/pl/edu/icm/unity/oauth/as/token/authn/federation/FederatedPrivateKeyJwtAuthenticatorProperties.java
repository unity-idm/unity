/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn.federation;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.oauth.as.token.authn.JwtClientAssertionVerifier;

class FederatedPrivateKeyJwtAuthenticatorProperties extends UnityPropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH,
			FederatedPrivateKeyJwtAuthenticatorProperties.class);
	@DocumentationReferencePrefix
	static final String PREFIX = "unity.federatedPrivateKeyJwtAuthenticator.";

	static final String ALLOWED_CLOCK_SKEW = "allowedClockSkew";
	static final String MAX_ASSERTION_LIFETIME = "maxAssertionLifetime";

	@DocumentationReferenceMeta
	static final Map<String, PropertyMD> META = new HashMap<>();
	static
	{
		META.put(ALLOWED_CLOCK_SKEW, new PropertyMD(String.valueOf(JwtClientAssertionVerifier.DEFAULT_CLOCK_SKEW.toSeconds()))
				.setMin(0)
				.setDescription("Allowed clock skew (in seconds) when validating the exp, iat and nbf "
						+ "claims of the client JWT assertion."));
		META.put(MAX_ASSERTION_LIFETIME, new PropertyMD(
				String.valueOf(JwtClientAssertionVerifier.DEFAULT_MAX_ASSERTION_LIFETIME.toSeconds()))
				.setMin(0)
				.setDescription("Maximum allowed lifetime (in seconds, exp - iat) of the client JWT "
						+ "assertion (RFC 7523 §3)."));
	}

	FederatedPrivateKeyJwtAuthenticatorProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}

	Properties getProperties()
	{
		return properties;
	}
}
