/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz.externalScript.input;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ExternalAuthorizationScriptInput.Builder.class)
public class ExternalAuthorizationScriptInput
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

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private List<InputAttribute> attributes = Collections.emptyList();
		private List<InputIdentity> identities = Collections.emptyList();
		private InputRequest request;

		private Builder()
		{
		}

		public Builder withAttributes(List<InputAttribute> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public Builder withIdentities(List<InputIdentity> identities)
		{
			this.identities = identities;
			return this;
		}

		public Builder withRequest(InputRequest request)
		{
			this.request = request;
			return this;
		}

		public ExternalAuthorizationScriptInput build()
		{
			return new ExternalAuthorizationScriptInput(this);
		}
	}
}
