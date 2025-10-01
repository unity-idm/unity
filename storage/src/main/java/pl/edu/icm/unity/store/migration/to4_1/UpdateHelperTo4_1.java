package pl.edu.icm.unity.store.migration.to4_1;

import com.fasterxml.jackson.databind.node.ObjectNode;
import pl.edu.icm.unity.base.exceptions.InternalException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Properties;

class UpdateHelperTo4_1
{
	private static final String LOCAL_OAUTH_RP_VERIFICATOR = "local-oauth-rp";
	private static final String LEGACY_CREDENTIAL_PROPERTY = "unity.oauth2-local-rp.credential";

	private UpdateHelperTo4_1()
	{
	}

	static Optional<ObjectNode> removeLocalOAuthCredential(ObjectNode authenticator)
	{
		if (authenticator == null || !authenticator.hasNonNull("verificationMethod")
				|| !LOCAL_OAUTH_RP_VERIFICATOR.equals(authenticator.get("verificationMethod").asText())
				|| !authenticator.hasNonNull("configuration"))
		{
			return Optional.empty();
		}

		String configuration = authenticator.get("configuration").asText();
		Optional<String> sanitized = sanitizeConfiguration(configuration);
		if (sanitized.isEmpty())
		{
			return Optional.empty();
		}

		authenticator.put("configuration", sanitized.get());
		return Optional.of(authenticator);
	}

	private static Optional<String> sanitizeConfiguration(String configuration)
	{
		Properties properties = parse(configuration);
		Object removed = properties.remove(LEGACY_CREDENTIAL_PROPERTY);
		if (removed == null)
		{
			return Optional.empty();
		}

		return Optional.of(serialize(properties));
	}

	private static Properties parse(String configuration)
	{
		Properties properties = new Properties();
		try
		{
			properties.load(new StringReader(configuration));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the local OAuth RP authenticator", e);
		}
		return properties;
	}

	private static String serialize(Properties properties)
	{
		StringWriter writer = new StringWriter();
		try
		{
			properties.store(writer, "");
		} catch (IOException e)
		{
			throw new InternalException("Unable to serialize local OAuth RP configuration", e);
		}
		return writer.toString();
	}
}
