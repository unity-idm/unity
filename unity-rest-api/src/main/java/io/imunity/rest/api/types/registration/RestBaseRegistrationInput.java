/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.imunity.rest.api.types.basic.RestAttribute;
import io.imunity.rest.api.types.basic.RestIdentityParam;
import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementDecision;

public class RestBaseRegistrationInput
{
	@JsonProperty("FormId")
	public final String formId;
	@JsonProperty("Identities")
	public final List<RestIdentityParam> identities;
	@JsonProperty("Attributes")
	public final List<RestAttribute> attributes;
	@JsonProperty("Credentials")
	public final List<RestCredentialParamValue> credentials;
	@JsonProperty("GroupSelections")
	public final List<RestGroupSelection> groupSelections;
	@JsonProperty("Agreements")
	public final List<RestSelection> agreements;
	@JsonProperty("PolicyAgreements")
	public final List<RestPolicyAgreementDecision> policyAgreements;
	@JsonProperty("Comments")
	public final String comments;
	@JsonProperty("UserLocale")
	public final String userLocale;
	@JsonProperty("RegistrationCode")
	public final String registrationCode;

	protected RestBaseRegistrationInput(RestBaseRegistrationInputBuilder<?> builder)
	{
		this.formId = builder.formId;
		this.identities = Optional.ofNullable(builder.identities)
				.map(List::copyOf)
				.orElse(null);
		this.attributes = Optional.ofNullable(builder.attributes)
				.map(List::copyOf)
				.orElse(null);
		this.credentials = Optional.ofNullable(builder.credentials)
				.map(List::copyOf)
				.orElse(null);
		this.groupSelections = Optional.ofNullable(builder.groupSelections)
				.map(List::copyOf)
				.orElse(null);
		this.agreements = Optional.ofNullable(builder.agreements)
				.map(List::copyOf)
				.orElse(null);
		this.policyAgreements = Optional.ofNullable(builder.policyAgreements)
				.map(List::copyOf)
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
		RestBaseRegistrationInput other = (RestBaseRegistrationInput) obj;
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
		private List<RestIdentityParam> identities = Collections.emptyList();
		@JsonProperty("Attributes")
		private List<RestAttribute> attributes = Collections.emptyList();
		@JsonProperty("Credentials")
		private List<RestCredentialParamValue> credentials = Collections.emptyList();
		@JsonProperty("GroupSelections")
		private List<RestGroupSelection> groupSelections = Collections.emptyList();
		@JsonProperty("Agreements")
		private List<RestSelection> agreements = Collections.emptyList();
		@JsonProperty("PolicyAgreements")
		private List<RestPolicyAgreementDecision> policyAgreements = Collections.emptyList();
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
		public T withIdentities(List<RestIdentityParam> identities)
		{
			this.identities = Optional.ofNullable(identities)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withAttributes(List<RestAttribute> attributes)
		{
			this.attributes = Optional.ofNullable(attributes)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withCredentials(List<RestCredentialParamValue> credentials)
		{
			this.credentials = Optional.ofNullable(credentials)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withGroupSelections(List<RestGroupSelection> groupSelections)
		{
			this.groupSelections = Optional.ofNullable(groupSelections)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withAgreements(List<RestSelection> agreements)
		{
			this.agreements = Optional.ofNullable(agreements)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withPolicyAgreements(List<RestPolicyAgreementDecision> policyAgreements)
		{
			this.policyAgreements = Optional.ofNullable(policyAgreements)
					.map(List::copyOf)
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

		public RestBaseRegistrationInput build()
		{
			return new RestBaseRegistrationInput(this);
		}
	}

}
