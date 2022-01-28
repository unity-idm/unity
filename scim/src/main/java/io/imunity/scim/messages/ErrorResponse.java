/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.messages;

import static org.springframework.util.Assert.notEmpty;
import static org.springframework.util.Assert.notNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.scim.types.Schemas;
import pl.edu.icm.unity.Constants;

@JsonPropertyOrder(
{ "schemas", "status" })
@JsonDeserialize(builder = ErrorResponse.Builder.class)
public class ErrorResponse
{
	public enum ScimType
	{
		invalidFilter, tooMany, uniqueness, mutability, invalidSyntax, invalidPath, noTarget, invalidValue, invalidVers,
		sensitive
	}

	public static final String SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";

	public final Schemas schemas;
	public final int status;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public final ScimType scimType;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public final String detail;

	protected ErrorResponse(Builder builder)
	{
		this.schemas = builder.schemas;
		this.status = builder.status;
		this.scimType = builder.scimType;
		this.detail = builder.detail;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(detail, schemas, scimType, status);
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
		ErrorResponse other = (ErrorResponse) obj;
		return Objects.equals(detail, other.detail) && Objects.equals(schemas, other.schemas)
				&& scimType == other.scimType && status == other.status;
	}

	public String toJsonString()
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Shouldn't happen: can't serialize error to JSON string", e);
		}
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private ScimType scimType;
		private Integer status;
		private String detail;
		private Schemas schemas;

		public Builder()
		{
			withSchemas(Schemas.of(SCHEMA));
		}

		public Builder withSchemas(Schemas schemas)
		{
			this.schemas = schemas;
			return this;
		}

		public Builder withStatus(Integer status)
		{
			this.status = status;
			return this;
		}

		public Builder withScimType(ScimType scimType)
		{
			this.scimType = scimType;
			return this;
		}

		public Builder withDetail(String detail)
		{
			this.detail = detail;
			return this;
		}

		public ErrorResponse build()
		{
			notNull(schemas, "schemas cannot be null.");
			notEmpty(schemas, "schemas cannot be empty.");
			notNull(status, "status cannot be null.");
			return new ErrorResponse(this);
		}
	}

}
