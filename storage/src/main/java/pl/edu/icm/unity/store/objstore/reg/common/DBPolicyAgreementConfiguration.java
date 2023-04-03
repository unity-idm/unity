/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.types.common.DBI18nString;

@JsonDeserialize(builder = DBPolicyAgreementConfiguration.Builder.class)
class DBPolicyAgreementConfiguration
{
	public final List<Long> documentsIdsToAccept;
	public final String presentationType;
	public final DBI18nString text;

	private DBPolicyAgreementConfiguration(Builder builder)
	{
		this.documentsIdsToAccept = Optional.ofNullable(builder.documentsIdsToAccept)
				.map(List::copyOf)
				.orElse(null);
		this.presentationType = builder.presentationType;
		this.text = builder.text;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(documentsIdsToAccept, presentationType, text);
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
		DBPolicyAgreementConfiguration other = (DBPolicyAgreementConfiguration) obj;
		return Objects.equals(documentsIdsToAccept, other.documentsIdsToAccept)
				&& Objects.equals(presentationType, other.presentationType) && Objects.equals(text, other.text);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private List<Long> documentsIdsToAccept = Collections.emptyList();
		private String presentationType;
		private DBI18nString text;

		private Builder()
		{
		}

		public Builder withDocumentsIdsToAccept(List<Long> documentsIdsToAccept)
		{
			this.documentsIdsToAccept = Optional.ofNullable(documentsIdsToAccept)
					.map(List::copyOf)
					.orElse(null);
			return this;
		}

		public Builder withPresentationType(String presentationType)
		{
			this.presentationType = presentationType;
			return this;
		}

		public Builder withText(DBI18nString text)
		{
			this.text = text;
			return this;
		}

		public DBPolicyAgreementConfiguration build()
		{
			return new DBPolicyAgreementConfiguration(this);
		}
	}

}
