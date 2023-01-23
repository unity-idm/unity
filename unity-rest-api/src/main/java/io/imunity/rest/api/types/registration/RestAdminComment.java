/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestAdminComment.Builder.class)
public class RestAdminComment
{
	public final long authorEntityId;
	public final boolean publicComment;
	public final Date date;
	public final String contents;

	private RestAdminComment(Builder builder)
	{
		this.authorEntityId = builder.authorEntityId;
		this.publicComment = builder.publicComment;
		this.date = builder.date;
		this.contents = builder.contents;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(authorEntityId, contents, date, publicComment);
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
		RestAdminComment other = (RestAdminComment) obj;
		return authorEntityId == other.authorEntityId && Objects.equals(contents, other.contents)
				&& Objects.equals(date, other.date) && publicComment == other.publicComment;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private long authorEntityId;
		private boolean publicComment;
		private Date date;
		private String contents;

		private Builder()
		{
		}

		public Builder withAuthorEntityId(long authorEntityId)
		{
			this.authorEntityId = authorEntityId;
			return this;
		}

		public Builder withPublicComment(boolean publicComment)
		{
			this.publicComment = publicComment;
			return this;
		}

		public Builder withDate(Date date)
		{
			this.date = date;
			return this;
		}

		public Builder withContents(String contents)
		{
			this.contents = contents;
			return this;
		}

		public RestAdminComment build()
		{
			return new RestAdminComment(this);
		}
	}

}
