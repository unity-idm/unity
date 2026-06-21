/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public class OAuthProviderKey
{
	static final String FEDERATION_PREFIX = "_fed_";

	private final String key;

	private OAuthProviderKey(String key)
	{
		this.key = key;
	}

	public static OAuthProviderKey fromConfig(String configurationEntryPrefix)
	{
		if (!configurationEntryPrefix.startsWith(OAuthClientProperties.PROVIDERS)
				|| !configurationEntryPrefix.endsWith("."))
			throw new IllegalArgumentException(configurationEntryPrefix
					+ " doesn't look like oauth provider config prefix");
		return new OAuthProviderKey(configurationEntryPrefix.substring(
				OAuthClientProperties.PROVIDERS.length(),
				configurationEntryPrefix.length() - 1));
	}

	public static OAuthProviderKey of(String key)
	{
		return new OAuthProviderKey(key);
	}

	public static OAuthProviderKey fromFederationEntity(String entityId)
	{
		return new OAuthProviderKey(FEDERATION_PREFIX + md5(entityId));
	}

	public boolean isFromFederation()
	{
		return key.startsWith(FEDERATION_PREFIX);
	}

	private static String md5(String input)
	{
		try
		{
			byte[] digest = MessageDigest.getInstance("MD5")
					.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
	}
	
	public String asString()
	{
		return key;
	}

	@Override
	public String toString()
	{
		return String.format("OAuthProviderKey [key=%s]", key);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(key);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OAuthProviderKey other = (OAuthProviderKey) obj;
		return Objects.equals(key, other.key);
	}
}
