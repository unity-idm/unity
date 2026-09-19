/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.authn;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata.Protocol;

@JsonDeserialize(builder = SerializableRemoteAuthnMetadata.Builder.class)
public class SerializableRemoteAuthnMetadata
{
	public final Protocol protocol;
	public final String remoteIdPId;
	public final List<String> classReferences;

	private SerializableRemoteAuthnMetadata(Builder builder)
	{
		this.protocol = builder.protocol;
		this.remoteIdPId = builder.remoteIdPId;
		this.classReferences = List.copyOf(builder.classReferences);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(classReferences, protocol, remoteIdPId);
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
		SerializableRemoteAuthnMetadata other = (SerializableRemoteAuthnMetadata) obj;
		return Objects.equals(classReferences, other.classReferences) && protocol == other.protocol
				&& Objects.equals(remoteIdPId, other.remoteIdPId);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Protocol protocol;
		private String remoteIdPId;
		private List<String> classReferences = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withProtocol(Protocol protocol)
		{
			this.protocol = protocol;
			return this;
		}

		public Builder withRemoteIdPId(String remoteIdPId)
		{
			this.remoteIdPId = remoteIdPId;
			return this;
		}

		public Builder withClassReferences(List<String> classReferences)
		{
			this.classReferences = classReferences;
			return this;
		}

		public SerializableRemoteAuthnMetadata build()
		{
			return new SerializableRemoteAuthnMetadata(this);
		}
	}	
}