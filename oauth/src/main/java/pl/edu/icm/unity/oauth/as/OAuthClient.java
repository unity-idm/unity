/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.nimbusds.oauth2.sdk.client.ClientType;

import pl.edu.icm.unity.types.basic.AttributeExt;

public class OAuthClient
{
	public final String username;
	public final long entityId;
	public final Map<String, AttributeExt> attributes;

	public OAuthClient(String username, long entityId, Map<String, AttributeExt> attributes)
	{
		this.username = username;
		this.entityId = entityId;
		this.attributes = attributes;
	}

	public Optional<String> getName()
	{
		return attributes.get(OAuthSystemAttributesProvider.CLIENT_NAME) != null
				? Optional.of(attributes.get(OAuthSystemAttributesProvider.CLIENT_NAME).getValues().get(0))
				: Optional.empty();
	}

	public Optional<AttributeExt> getLogo()
	{
		return attributes.get(OAuthSystemAttributesProvider.CLIENT_LOGO) != null
				? Optional.of(attributes.get(OAuthSystemAttributesProvider.CLIENT_LOGO))
				: Optional.empty();
	}

	public Optional<String> getGroup()
	{
		return attributes.get(OAuthSystemAttributesProvider.PER_CLIENT_GROUP) != null
				? Optional.of(attributes.get(OAuthSystemAttributesProvider.PER_CLIENT_GROUP).getValues().get(0))
				: Optional.empty();
	}

	public Optional<AttributeExt> getAllowedUrisA()
	{
		return attributes.get(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI) != null
				? Optional.of(attributes.get(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI))
				: Optional.empty();
	}

	public Optional<ClientType> getType()
	{
		return attributes.get(OAuthSystemAttributesProvider.CLIENT_TYPE) != null
				? Optional.of(ClientType
						.valueOf(attributes.get(OAuthSystemAttributesProvider.CLIENT_TYPE).getValues().get(0)))
				: Optional.empty();
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entityId, attributes, username);
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
		OAuthClient other = (OAuthClient) obj;
		return entityId == other.entityId && Objects.equals(attributes, other.attributes)
				&& Objects.equals(username, other.username);
	}
}
