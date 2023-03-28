/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import pl.edu.icm.unity.store.impl.attribute.DBAttribute;

public class DBBaseRegistrationInput
{
	@JsonProperty("FormId")
	public final String formId;
	@JsonProperty("Identities")
	public final List<DBIdentityParam> identities;
	@JsonProperty("Attributes")
	public final List<DBAttribute> attributes;
	@JsonProperty("Credentials")
	public final List<DBCredentialParamValue> credentials;
	@JsonProperty("GroupSelections")
	public final List<DBGroupSelection> groupSelections;
	@JsonProperty("Agreements")
	public final List<DBSelection> agreements;
	@JsonProperty("PolicyAgreements")
	public final List<DBPolicyAgreementDecision> policyAgreements;
	@JsonProperty("Comments")
	public final String comments;
	@JsonProperty("UserLocale")
	public final String userLocale;
	@JsonProperty("RegistrationCode")
	public final String registrationCode;

	protected DBBaseRegistrationInput(RestBaseRegistrationInputBuilder<?> builder)
	{
		this.formId = builder.formId;
		this.identities = Optional.ofNullable(builder.identities)
				.map(ArrayList::new)
				.map(Collections::unmodifiableList)
				.orElse(null);
		this.attributes = Optional.ofNullable(builder.attributes)
				.map(ArrayList::new)
				.map(Collections::unmodifiableList)
				.orElse(null);
		this.credentials = Optional.ofNullable(builder.credentials)
				.map(ArrayList::new)
				.map(Collections::unmodifiableList)
				.orElse(null);
		this.groupSelections = Optional.ofNullable(builder.groupSelections)
				.map(ArrayList::new)
				.map(Collections::unmodifiableList)
				.orElse(null);
		this.agreements = Optional.ofNullable(builder.agreements)
				.map(ArrayList::new)
				.map(Collections::unmodifiableList)
				.orElse(null);
		this.policyAgreements = Optional.ofNullable(builder.policyAgreements)
				.map(ArrayList::new)
				.map(Collections::unmodifiableList)
				.orElse(null);
		this.comments = builder.comments;
		this.userLocale = builder.userLocale;
		this.registrationCode = builder.registrationCode;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(agreements, attributes, comments, credentials, formId, groupSelections, identities,
				policyAgreements, registrationCode, userLocale);
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
		DBBaseRegistrationInput other = (DBBaseRegistrationInput) obj;
		return Objects.equals(agreements, other.agreements) && Objects.equals(attributes, other.attributes)
				&& Objects.equals(comments, other.comments) && Objects.equals(credentials, other.credentials)
				&& Objects.equals(formId, other.formId) && Objects.equals(groupSelections, other.groupSelections)
				&& Objects.equals(identities, other.identities)
				&& Objects.equals(policyAgreements, other.policyAgreements)
				&& Objects.equals(registrationCode, other.registrationCode)
				&& Objects.equals(userLocale, other.userLocale);
	}

	public static class RestBaseRegistrationInputBuilder<T extends RestBaseRegistrationInputBuilder<?>>
	{
		@JsonProperty("FormId")
		private String formId;
		@JsonProperty("Identities")
		private List<DBIdentityParam> identities = Collections.emptyList();
		@JsonProperty("Attributes")
		private List<DBAttribute> attributes = Collections.emptyList();
		@JsonProperty("Credentials")
		private List<DBCredentialParamValue> credentials = Collections.emptyList();
		@JsonProperty("GroupSelections")
		private List<DBGroupSelection> groupSelections = Collections.emptyList();
		@JsonProperty("Agreements")
		private List<DBSelection> agreements = Collections.emptyList();
		@JsonProperty("PolicyAgreements")
		private List<DBPolicyAgreementDecision> policyAgreements = Collections.emptyList();
		@JsonProperty("Comments")
		private String comments;
		@JsonProperty("UserLocale")
		private String userLocale;
		@JsonProperty("RegistrationCode")
		private String registrationCode;

		protected RestBaseRegistrationInputBuilder()
		{
		}

		@SuppressWarnings("unchecked")
		public T withFormId(String formId)
		{
			this.formId = formId;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withIdentities(List<DBIdentityParam> identities)
		{
			this.identities = Optional.ofNullable(identities)
					.map(ArrayList::new)
					.map(Collections::unmodifiableList)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withAttributes(List<DBAttribute> attributes)
		{
			this.attributes = Optional.ofNullable(attributes)
					.map(ArrayList::new)
					.map(Collections::unmodifiableList)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withCredentials(List<DBCredentialParamValue> credentials)
		{
			this.credentials = Optional.ofNullable(credentials)
					.map(ArrayList::new)
					.map(Collections::unmodifiableList)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withGroupSelections(List<DBGroupSelection> groupSelections)
		{
			this.groupSelections = Optional.ofNullable(groupSelections)
					.map(Collections::unmodifiableList)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withAgreements(List<DBSelection> agreements)
		{
			this.agreements = Optional.ofNullable(agreements)
					.map(ArrayList::new)
					.map(Collections::unmodifiableList)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withPolicyAgreements(List<DBPolicyAgreementDecision> policyAgreements)
		{
			this.policyAgreements = Optional.ofNullable(policyAgreements)
					.map(ArrayList::new)
					.map(Collections::unmodifiableList)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withComments(String comments)
		{
			this.comments = comments;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withUserLocale(String userLocale)
		{
			this.userLocale = userLocale;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withRegistrationCode(String registrationCode)
		{
			this.registrationCode = registrationCode;
			return (T) this;
		}

		public DBBaseRegistrationInput build()
		{
			return new DBBaseRegistrationInput(this);
		}
	}

}
