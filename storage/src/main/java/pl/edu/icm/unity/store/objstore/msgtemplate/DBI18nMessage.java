/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.msgtemplate;

import java.util.Objects;

import pl.edu.icm.unity.store.types.DBI18nString;

class DBI18nMessage
{
	public final DBI18nString body;
	public final DBI18nString subject;

	@Override
	public int hashCode()
	{
		return Objects.hash(body, subject);
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
		DBI18nMessage other = (DBI18nMessage) obj;
		return Objects.equals(body, other.body) && Objects.equals(subject, other.subject);
	}

	private DBI18nMessage(Builder builder)
	{
		this.body = builder.body;
		this.subject = builder.subject;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private DBI18nString body;
		private DBI18nString subject;

		private Builder()
		{
		}

		public Builder withBody(DBI18nString body)
		{
			this.body = body;
			return this;
		}

		public Builder withSubject(DBI18nString subject)
		{
			this.subject = subject;
			return this;
		}

		public DBI18nMessage build()
		{
			return new DBI18nMessage(this);
		}
	}

}