/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.policyDocuments;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBPolicyDocument.DBPolicyDocumentBuilder.class)
class DBPolicyDocument extends DBPolicyDocumentBase
{
	public final Long id;
	public final String name;

	DBPolicyDocument(DBPolicyDocumentBuilder builder)
	{
		super(builder);
		this.id = builder.id;
		this.name = builder.name;

	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(id, name);
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
		DBPolicyDocument other = (DBPolicyDocument) obj;
		return Objects.equals(id, other.id) && Objects.equals(name, other.name);
	}

	public static DBPolicyDocumentBuilder builder()
	{
		return new DBPolicyDocumentBuilder();
	}

	public static final class DBPolicyDocumentBuilder extends DBPolicyDocumentBaseBuilder<DBPolicyDocumentBuilder>
	{
		private Long id;
		private String name;

		private DBPolicyDocumentBuilder()
		{
		}

		public DBPolicyDocumentBuilder withId(Long id)
		{
			this.id = id;
			return this;
		}

		public DBPolicyDocumentBuilder withName(String name)
		{
			this.name = name;
			return this;
		}

		public DBPolicyDocument build()
		{
			return new DBPolicyDocument(this);
		}
	}
}
