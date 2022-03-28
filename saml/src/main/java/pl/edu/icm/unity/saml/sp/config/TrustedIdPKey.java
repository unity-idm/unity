/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.config;

import java.util.Objects;

import org.apache.commons.codec.digest.DigestUtils;

import pl.edu.icm.unity.saml.sp.SAMLSPProperties;

public class TrustedIdPKey
{
	private final String key;
	
	public TrustedIdPKey(String key)
	{
		this.key = key;
	}

	public static TrustedIdPKey individuallyConfigured(String configurationEntryPrefix)
	{
		if (!configurationEntryPrefix.startsWith(SAMLSPProperties.IDP_PREFIX) || !configurationEntryPrefix.endsWith("."))
			throw new IllegalArgumentException(configurationEntryPrefix + " doesn't look like trusted idp config prefix");
		return new TrustedIdPKey(configurationEntryPrefix.substring(SAMLSPProperties.IDP_PREFIX.length(),
				configurationEntryPrefix.length()-1));
	}
	
	public static TrustedIdPKey metadataEntity(String samlEntityId, int index)
	{
		String entityHex = DigestUtils.md5Hex(samlEntityId);
		return new TrustedIdPKey("_entryFromMetadata_" + entityHex + "+" + index + ".");
	}
	
	public String asString()
	{
		return key;
	}
	
	@Override
	public String toString()
	{
		return String.format("TrustedIdPKey [key=%s]", key);
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
		TrustedIdPKey other = (TrustedIdPKey) obj;
		return Objects.equals(key, other.key);
	}
}
