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

	static final String ALLOWED_CLOCK_SKEW = "allowedClockSkewSeconds";

	@DocumentationReferenceMeta
	static final Map<String, PropertyMD> META = new HashMap<>();
	static
	{
		META.put(ALLOWED_CLOCK_SKEW, new PropertyMD(String.valueOf(JwtClientAssertionVerifier.DEFAULT_CLOCK_SKEW.toSeconds()))
				.setBounds(0, JwtClientAssertionVerifier.MAX_ASSERTION_LIFETIME.toSeconds())
				.setDescription("Allowed clock skew (in seconds) when validating the exp, iat and nbf "
						+ "claims of the client JWT assertion."));
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
