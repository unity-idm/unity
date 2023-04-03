/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.policyDocuments;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.types.common.DBI18nString;

@JsonDeserialize(builder = DBPolicyDocumentBase.DBPolicyDocumentBaseBuilder.class)
class DBPolicyDocumentBase
{
	public final DBI18nString displayedName;
	public final boolean mandatory;
	public final String contentType;
	public final int revision;
	public final DBI18nString content;

	protected DBPolicyDocumentBase(DBPolicyDocumentBaseBuilder<?> builder)
	{
		this.displayedName = builder.displayedName;
		this.mandatory = builder.mandatory;
		this.contentType = builder.contentType;
		this.revision = builder.revision;
		this.content = builder.content;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(content, contentType, displayedName, mandatory, revision);
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
		DBPolicyDocumentBase other = (DBPolicyDocumentBase) obj;
		return Objects.equals(content, other.content) && Objects.equals(contentType, other.contentType)
				&& Objects.equals(displayedName, other.displayedName) && mandatory == other.mandatory
				&& revision == other.revision;
	}

	public static DBPolicyDocumentBaseBuilder<?> builder()
	{
		return new DBPolicyDocumentBaseBuilder<>();
	}

	public static  class DBPolicyDocumentBaseBuilder<T extends DBPolicyDocumentBaseBuilder<?>>
	{
		private DBI18nString displayedName;
		private boolean mandatory;
		private String contentType;
		private int revision;
		private DBI18nString content;

		protected DBPolicyDocumentBaseBuilder()
		{
		}

		@SuppressWarnings("unchecked")
		public T withDisplayedName(DBI18nString displayedName)
		{
			this.displayedName = displayedName;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withMandatory(boolean mandatory)
		{
			this.mandatory = mandatory;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withContentType(String contentType)
		{
			this.contentType = contentType;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withRevision(int revision)
		{
			this.revision = revision;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withContent(DBI18nString content)
		{
			this.content = content;
			return (T) this;
		}

		public DBPolicyDocumentBase build()
		{
			return new DBPolicyDocumentBase(this);
		}
	}

}
