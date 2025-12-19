/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz.externalScript;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ExternalAuthorizationScriptInput.Builder.class)
class ExternalAuthorizationScriptInput
{
	public final Collection<InputAttribute> attributes;
	public final Collection<InputIdentity> identities;
	public final InputRequest request;

	private ExternalAuthorizationScriptInput(Builder builder)
	{
		this.attributes = Optional.ofNullable(builder.attributes)
				.map(List::copyOf)
				.orElse(Collections.emptyList());
		this.identities = Optional.ofNullable(builder.identities)
				.map(List::copyOf)
				.orElse(Collections.emptyList());
		this.request = builder.request;
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private List<InputAttribute> attributes = Collections.emptyList();
		private List<InputIdentity> identities = Collections.emptyList();
		private InputRequest request;

		private Builder()
		{
		}

		Builder withAttributes(List<InputAttribute> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		Builder withIdentities(List<InputIdentity> identities)
		{
			this.identities = identities;
			return this;
		}

		Builder withRequest(InputRequest request)
		{
			this.request = request;
			return this;
		}

		ExternalAuthorizationScriptInput build()
		{
			return new ExternalAuthorizationScriptInput(this);
		}
	}
}
