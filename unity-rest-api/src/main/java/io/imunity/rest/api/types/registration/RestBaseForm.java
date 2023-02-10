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

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementConfiguration;
import io.imunity.rest.api.types.registration.layout.RestFormLayoutSettings;
import io.imunity.rest.api.types.translation.RestTranslationProfile;

public class RestBaseForm
{
	@JsonProperty("Name")
	public final String name;
	@JsonProperty("Description")
	public final String description;
	@JsonProperty("IdentityParams")
	public final List<RestIdentityRegistrationParam> identityParams;
	@JsonProperty("AttributeParams")
	public final List<RestAttributeRegistrationParam> attributeParams;
	@JsonProperty("GroupParams")
	public final List<RestGroupRegistrationParam> groupParams;
	@JsonProperty("CredentialParams")
	public final List<RestCredentialRegistrationParam> credentialParams;
	@JsonProperty("Agreements")
	public final List<RestAgreementRegistrationParam> agreements;
	@JsonProperty("CollectComments")
	public final boolean collectComments;
	@JsonProperty("DisplayedName")
	public final RestI18nString displayedName;
	@JsonProperty("i18nFormInformation")
	public final RestI18nString i18nFormInformation;
	@JsonProperty("FormInformation")
	public final String formInformation;
	@JsonProperty("PageTitle")
	public final RestI18nString pageTitle;
	@JsonProperty("TranslationProfile")
	public final RestTranslationProfile translationProfile;
	@JsonProperty("FormLayoutSettings")
	public final RestFormLayoutSettings layoutSettings;
	@JsonProperty("WrapUpConfig")
	public final List<RestRegistrationWrapUpConfig> wrapUpConfig;
	@JsonProperty("PolicyAgreements")
	public final List<RestPolicyAgreementConfiguration> policyAgreements;
	@JsonProperty("ByInvitationOnly")
	public final boolean byInvitationOnly;
	@JsonProperty("CheckIdentityOnSubmit")
	public final boolean checkIdentityOnSubmit;

	protected RestBaseForm(RestBaseFormBuilder<?> builder)
	{
		this.name = builder.name;
		this.description = builder.description;
		this.identityParams = Optional.ofNullable(builder.identityParams)
				.map(List::copyOf)
				.orElse(null);
		this.attributeParams = Optional.ofNullable(builder.attributeParams)
				.map(List::copyOf)
				.orElse(null);
		this.groupParams = Optional.ofNullable(builder.groupParams)
				.map(List::copyOf)
				.orElse(null);
		this.credentialParams = Optional.ofNullable(builder.credentialParams)
				.map(List::copyOf)
				.orElse(null);
		this.agreements = Optional.ofNullable(builder.agreements)
				.map(List::copyOf)
				.orElse(null);
		this.collectComments = builder.collectComments;
		this.displayedName = builder.displayedName;
		this.i18nFormInformation = builder.i18nFormInformation;
		this.pageTitle = builder.pageTitle;
		this.translationProfile = builder.translationProfile;
		this.layoutSettings = builder.layoutSettings;
		this.wrapUpConfig = Optional.ofNullable(builder.wrapUpConfig)
				.map(List::copyOf)
				.orElse(null);
		this.policyAgreements = Optional.ofNullable(builder.policyAgreements)
				.map(List::copyOf)
				.orElse(null);
		this.byInvitationOnly = builder.byInvitationOnly;
		this.checkIdentityOnSubmit = builder.checkIdentityOnSubmit;
		this.formInformation = builder.formInformation;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(agreements, attributeParams, byInvitationOnly, checkIdentityOnSubmit, collectComments,
				credentialParams, description, displayedName, i18nFormInformation, groupParams, identityParams,
				layoutSettings, name, pageTitle, policyAgreements, translationProfile, wrapUpConfig, formInformation);
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
		RestBaseForm other = (RestBaseForm) obj;
		return Objects.equals(agreements, other.agreements) && Objects.equals(attributeParams, other.attributeParams)
				&& byInvitationOnly == other.byInvitationOnly && checkIdentityOnSubmit == other.checkIdentityOnSubmit
				&& collectComments == other.collectComments && Objects.equals(credentialParams, other.credentialParams)
				&& Objects.equals(description, other.description) && Objects.equals(displayedName, other.displayedName)
				&& Objects.equals(i18nFormInformation, other.i18nFormInformation)
				&& Objects.equals(groupParams, other.groupParams)
				&& Objects.equals(identityParams, other.identityParams)
				&& Objects.equals(layoutSettings, other.layoutSettings) && Objects.equals(name, other.name)
				&& Objects.equals(pageTitle, other.pageTitle)
				&& Objects.equals(policyAgreements, other.policyAgreements)
				&& Objects.equals(translationProfile, other.translationProfile)
				&& Objects.equals(wrapUpConfig, other.wrapUpConfig)
				&& Objects.equals(i18nFormInformation, other.i18nFormInformation)
				&& Objects.equals(formInformation, other.formInformation);
	}

	public static class RestBaseFormBuilder<T extends RestBaseFormBuilder<?>>
	{
		@JsonProperty("Name")
		private String name;
		@JsonProperty("Description")
		private String description;
		@JsonProperty("IdentityParams")
		private List<RestIdentityRegistrationParam> identityParams = Collections.emptyList();
		@JsonProperty("AttributeParams")
		private List<RestAttributeRegistrationParam> attributeParams = Collections.emptyList();
		@JsonProperty("GroupParams")
		private List<RestGroupRegistrationParam> groupParams = Collections.emptyList();
		@JsonProperty("CredentialParams")
		private List<RestCredentialRegistrationParam> credentialParams = Collections.emptyList();
		@JsonProperty("Agreements")
		private List<RestAgreementRegistrationParam> agreements = Collections.emptyList();
		@JsonProperty("CollectComments")
		private boolean collectComments;
		@JsonProperty("DisplayedName")
		private RestI18nString displayedName = RestI18nString.builder()
				.build();
		@JsonProperty("i18nFormInformation")
		private RestI18nString i18nFormInformation;
		@JsonProperty("FormInformation")
		private String formInformation;
		@JsonProperty("PageTitle")
		private RestI18nString pageTitle = RestI18nString.builder()
				.build();
		@JsonProperty("TranslationProfile")
		private RestTranslationProfile translationProfile = RestTranslationProfile.builder()
				.withName("registrationProfile")
				.withDescription("")
				.withType("REGISTRATION")
				.withRules(Collections.emptyList())
				.build();
		@JsonProperty("FormLayoutSettings")
		private RestFormLayoutSettings layoutSettings;
		@JsonProperty("WrapUpConfig")
		private List<RestRegistrationWrapUpConfig> wrapUpConfig = Collections.emptyList();
		@JsonProperty("PolicyAgreements")
		private List<RestPolicyAgreementConfiguration> policyAgreements = Collections.emptyList();
		@JsonProperty("ByInvitationOnly")
		private boolean byInvitationOnly;
		@JsonProperty("CheckIdentityOnSubmit")
		private boolean checkIdentityOnSubmit;

		protected RestBaseFormBuilder()
		{
		}

		@SuppressWarnings("unchecked")
		public T withName(String name)
		{
			this.name = name;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withDescription(String description)
		{
			this.description = description;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withIdentityParams(List<RestIdentityRegistrationParam> identityParams)
		{
			this.identityParams = Optional.ofNullable(identityParams)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withAttributeParams(List<RestAttributeRegistrationParam> attributeParams)
		{
			this.attributeParams = Optional.ofNullable(attributeParams)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withGroupParams(List<RestGroupRegistrationParam> groupParams)
		{
			this.groupParams = Optional.ofNullable(groupParams)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withCredentialParams(List<RestCredentialRegistrationParam> credentialParams)
		{
			this.credentialParams = Optional.ofNullable(credentialParams)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withAgreements(List<RestAgreementRegistrationParam> agreements)
		{
			this.agreements = Optional.ofNullable(agreements)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withCollectComments(boolean collectComments)
		{
			this.collectComments = collectComments;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withDisplayedName(RestI18nString displayedName)
		{
			this.displayedName = displayedName;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withFormInformation(RestI18nString formInformation)
		{
			this.i18nFormInformation = formInformation;
			return (T) this;
		}
		
		@SuppressWarnings("unchecked")
		public T withFormInformation(String formInformation)
		{
			this.formInformation= formInformation;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withPageTitle(RestI18nString pageTitle)
		{
			this.pageTitle = pageTitle;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withTranslationProfile(RestTranslationProfile translationProfile)
		{
			this.translationProfile = translationProfile;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withLayoutSettings(RestFormLayoutSettings layoutSettings)
		{
			this.layoutSettings = layoutSettings;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withWrapUpConfig(List<RestRegistrationWrapUpConfig> wrapUpConfig)
		{
			this.wrapUpConfig = Optional.ofNullable(wrapUpConfig)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withPolicyAgreements(List<RestPolicyAgreementConfiguration> policyAgreements)
		{
			this.policyAgreements = Optional.ofNullable(policyAgreements)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withByInvitationOnly(boolean byInvitationOnly)
		{
			this.byInvitationOnly = byInvitationOnly;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withCheckIdentityOnSubmit(boolean checkIdentityOnSubmit)
		{
			this.checkIdentityOnSubmit = checkIdentityOnSubmit;
			return (T) this;
		}

		public RestBaseForm build()
		{
			return new RestBaseForm(this);
		}
	}

}
