/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema.providerConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.imunity.scim.common.BasicSCIMResource;

public class SCIMProviderConfigResource extends BasicSCIMResource
{
	static final String SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig";

	public final String documentationUri;
	public final Supported patch;
	public final Supported bulk;
	public final Supported filter;
	public final Supported changePassword;
	public final Supported etag;
	public final List<AuthenticationSchema> authenticationSchemes;

	private SCIMProviderConfigResource(Builder builder)
	{
		super(builder);
		this.documentationUri = builder.documentationUri;
		this.patch = builder.patch;
		this.bulk = builder.bulk;
		this.filter = builder.filter;
		this.changePassword = builder.changePassword;
		this.etag = builder.etag;
		this.authenticationSchemes = builder.authenticationSchemes;
	}

	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ Objects.hash(authenticationSchemes, bulk, changePassword, documentationUri, etag, filter, patch);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SCIMProviderConfigResource other = (SCIMProviderConfigResource) obj;
		return Objects.equals(authenticationSchemes, other.authenticationSchemes) && Objects.equals(bulk, other.bulk)
				&& Objects.equals(changePassword, other.changePassword)
				&& Objects.equals(documentationUri, other.documentationUri) && Objects.equals(etag, other.etag)
				&& Objects.equals(filter, other.filter) && Objects.equals(patch, other.patch);
	}


	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder extends BasicScimResourceBuilder<Builder>
	{
		private String documentationUri;
		private Supported patch;
		private Supported bulk;
		private Supported filter;
		private Supported changePassword;
		private Supported etag;
		private List<AuthenticationSchema> authenticationSchemes = new ArrayList<>();

		public Builder()
		{
			withSchemas(Set.of(SCHEMA));
		}

		public SCIMProviderConfigResource build()
		{
			return new SCIMProviderConfigResource(this);
		}

		public Builder withDocumentationUri(String documentationUri)
		{
			this.documentationUri = documentationUri;
			return this;
		}

		public Builder withPatch(Supported patch)
		{
			this.patch = patch;
			return this;
		}

		public Builder withBulk(Supported bulk)
		{
			this.bulk = bulk;
			return this;
		}

		public Builder withFilter(Supported filter)
		{
			this.filter = filter;
			return this;
		}

		public Builder withChangePassword(Supported changePassword)
		{
			this.changePassword = changePassword;
			return this;
		}

		public Builder withEtag(Supported etag)
		{
			this.etag = etag;
			return this;
		}

		public Builder withAuthenticationSchemes(List<AuthenticationSchema> schemes)
		{
			this.authenticationSchemes = schemes;
			return this;
		}
	}

	public static class Supported
	{
		public final boolean supported;

		private Supported(Builder builder)
		{
			this.supported = builder.supported;
		}

		public static Builder builder()
		{
			return new Builder();
		}

		public static final class Builder
		{
			private boolean supported = false;

			private Builder()
			{
			}

			public Builder withSupported(boolean supported)
			{
				this.supported = supported;
				return this;
			}

			public Supported build()
			{
				return new Supported(this);
			}
		}
	}
	
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public static class AuthenticationSchema
	{
		public enum Type
		{
			oauth, oauth2, oauthbearertoken, httpbasic, httpdigest
		};

		public final String name;
		public final Type type;
		public final String description;
		public final String documentationUri;
		public final String specUri;

		private AuthenticationSchema(Builder builder)
		{
			this.name = builder.name;
			this.type = builder.type;
			this.description = builder.description;
			this.documentationUri = builder.documentationUri;
			this.specUri = builder.specUri;
		}

		public static Builder builder()
		{
			return new Builder();
		}

		public static final class Builder
		{
			private String name;
			private Type type;
			private String description;
			private String documentationUri;
			private String specUri;

			private Builder()
			{
			}

			public Builder withName(String name)
			{
				this.name = name;
				return this;
			}

			public Builder withType(Type type)
			{
				this.type = type;
				return this;
			}

			public Builder withDescription(String description)
			{
				this.description = description;
				return this;
			}

			public Builder withDocumentationUri(String documentationUri)
			{
				this.documentationUri = documentationUri;
				return this;
			}

			public Builder withSpecUri(String specUri)
			{
				this.specUri = specUri;
				return this;
			}

			public AuthenticationSchema build()
			{
				return new AuthenticationSchema(this);
			}
		}
	}

}
