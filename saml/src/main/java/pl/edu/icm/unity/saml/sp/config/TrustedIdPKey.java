/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.config;

import org.apache.commons.codec.digest.DigestUtils;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;

import java.util.Objects;
import java.util.Optional;

public class TrustedIdPKey
{
	private final String key;
	private final Optional<Metadata> metadata;

	public TrustedIdPKey(String key)
	{
		this.key = key;
		this.metadata = Optional.empty();
	}

	private TrustedIdPKey(String entityHex, int index)
	{
		this.key = "_entryFromMetadata_" + entityHex + "+" + index + ".";
		this.metadata = Optional.of(new Metadata(entityHex, index));
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
		return new TrustedIdPKey(entityHex, index);
	}

	public Optional<Metadata> getMetadata()
	{
		return metadata;
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

	public static class Metadata
	{
		public final String entityHex;
		public final int index;

		private Metadata(String entityHex, int index)
		{
			this.entityHex = entityHex;
			this.index = index;
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Metadata metadata = (Metadata) o;
			return index == metadata.index && Objects.equals(entityHex, metadata.entityHex);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(entityHex, index);
		}

		@Override
		public String toString()
		{
			return "Metadata{" +
					"entityHex='" + entityHex + '\'' +
					", index=" + index +
					'}';
		}
	}
}
