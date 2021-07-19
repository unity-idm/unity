/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.google.common.base.MoreObjects;

import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;

class OAuthContexts
{
	private final Map<ContextKey, OAuthAuthzContext> contexts;

	OAuthContexts()
	{
		this.contexts = new HashMap<>();
	}

	void put(ContextKey key, OAuthAuthzContext ctx)
	{
		contexts.put(key, ctx);
	}

	OAuthAuthzContext get(ContextKey ctxKey)
	{
		return contexts.get(ctxKey);
	}

	void remove(ContextKey key)
	{
		contexts.remove(key);
	}
	
	boolean isEmpty()
	{
		return contexts.isEmpty();
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this).add("contexts", contexts).toString();
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(contexts);
	}

	@Override
	public boolean equals(Object object)
	{
		if (object instanceof OAuthContexts)
		{
			OAuthContexts that = (OAuthContexts) object;
			return Objects.equals(this.contexts, that.contexts);
		}
		return false;
	}

	static class ContextKey
	{
		static final ContextKey DEFAULT = new ContextKey("default");

		final String key;

		ContextKey(String key)
		{
			this.key = key;
		}

		static ContextKey randomKey()
		{
			return new ContextKey(UUID.randomUUID().toString());
		}

		@Override
		public int hashCode()
		{
			return Objects.hashCode(key);
		}

		@Override
		public boolean equals(Object object)
		{
			if (object instanceof ContextKey)
			{
				ContextKey that = (ContextKey) object;
				return Objects.equals(this.key, that.key);
			}
			return false;
		}
	}
}
