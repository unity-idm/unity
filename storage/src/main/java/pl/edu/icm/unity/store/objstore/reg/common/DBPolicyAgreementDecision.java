/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBPolicyAgreementDecision.Builder.class)
public class DBPolicyAgreementDecision
{
	public final String acceptanceStatus;
	public final List<Long> documentsIdsToAccept;

	private DBPolicyAgreementDecision(Builder builder)
	{
		this.acceptanceStatus = builder.acceptanceStatus;
		this.documentsIdsToAccept = builder.documentsIdsToAccept;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(acceptanceStatus, documentsIdsToAccept);
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
		DBPolicyAgreementDecision other = (DBPolicyAgreementDecision) obj;
		return Objects.equals(acceptanceStatus, other.acceptanceStatus)
				&& Objects.equals(documentsIdsToAccept, other.documentsIdsToAccept);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String acceptanceStatus;
		private List<Long> documentsIdsToAccept = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withAcceptanceStatus(String acceptanceStatus)
		{
			this.acceptanceStatus = acceptanceStatus;
			return this;
		}

		public Builder withDocumentsIdsToAccept(List<Long> documentsIdsToAccept)
		{
			this.documentsIdsToAccept = documentsIdsToAccept;
			return this;
		}

		public DBPolicyAgreementDecision build()
		{
			return new DBPolicyAgreementDecision(this);
		}
	}

}
