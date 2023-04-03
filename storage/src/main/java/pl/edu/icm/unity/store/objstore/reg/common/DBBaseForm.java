/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import pl.edu.icm.unity.store.objstore.reg.layout.DBFormLayoutSettings;
import pl.edu.icm.unity.store.objstore.tprofile.DBTranslationProfile;
import pl.edu.icm.unity.store.types.common.DBI18nString;

public class DBBaseForm
{
	@JsonProperty("Name")
	public final String name;
	@JsonProperty("Description")
	public final String description;
	@JsonProperty("IdentityParams")
	public final List<DBIdentityRegistrationParam> identityParams;
	@JsonProperty("AttributeParams")
	public final List<DBAttributeRegistrationParam> attributeParams;
	@JsonProperty("GroupParams")
	public final List<DBGroupRegistrationParam> groupParams;
	@JsonProperty("CredentialParams")
	public final List<DBCredentialRegistrationParam> credentialParams;
	@JsonProperty("Agreements")
	public final List<DBAgreementRegistrationParam> agreements;
	@JsonProperty("CollectComments")
	public final boolean collectComments;
	@JsonProperty("DisplayedName")
	public final DBI18nString displayedName;
	@JsonProperty("i18nFormInformation")
	public final DBI18nString i18nFormInformation;
	@JsonProperty("FormInformation")
	public final String formInformation;
	@JsonProperty("PageTitle")
	public final DBI18nString pageTitle;
	@JsonProperty("TranslationProfile")
	public final DBTranslationProfile translationProfile;
	@JsonProperty("FormLayoutSettings")
	public final DBFormLayoutSettings layoutSettings;
	@JsonProperty("WrapUpConfig")
	public final List<DBRegistrationWrapUpConfig> wrapUpConfig;
	@JsonProperty("PolicyAgreements")
	public final List<DBPolicyAgreementConfiguration> policyAgreements;
	@JsonProperty("ByInvitationOnly")
	public final boolean byInvitationOnly;
	@JsonProperty("CheckIdentityOnSubmit")
	public final boolean checkIdentityOnSubmit;

	protected DBBaseForm(DBBaseFormBuilder<?> builder)
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
		DBBaseForm other = (DBBaseForm) obj;
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

	public static class DBBaseFormBuilder<T extends DBBaseFormBuilder<?>>
	{
		@JsonProperty("Name")
		private String name;
		@JsonProperty("Description")
		private String description;
		@JsonProperty("IdentityParams")
		private List<DBIdentityRegistrationParam> identityParams = Collections.emptyList();
		@JsonProperty("AttributeParams")
		private List<DBAttributeRegistrationParam> attributeParams = Collections.emptyList();
		@JsonProperty("GroupParams")
		private List<DBGroupRegistrationParam> groupParams = Collections.emptyList();
		@JsonProperty("CredentialParams")
		private List<DBCredentialRegistrationParam> credentialParams = Collections.emptyList();
		@JsonProperty("Agreements")
		private List<DBAgreementRegistrationParam> agreements = Collections.emptyList();
		@JsonProperty("CollectComments")
		private boolean collectComments;
		@JsonProperty("DisplayedName")
		private DBI18nString displayedName = DBI18nString.builder()
				.build();
		@JsonProperty("i18nFormInformation")
		private DBI18nString i18nFormInformation;
		@JsonProperty("FormInformation")
		private String formInformation;
		@JsonProperty("PageTitle")
		private DBI18nString pageTitle = DBI18nString.builder()
				.build();
		@JsonProperty("TranslationProfile")
		private DBTranslationProfile translationProfile = DBTranslationProfile.builder()
				.withName("registrationProfile")
				.withDescription("")
				.withType("REGISTRATION")
				.withRules(Collections.emptyList())
				.build();
		@JsonProperty("FormLayoutSettings")
		private DBFormLayoutSettings layoutSettings;
		@JsonProperty("WrapUpConfig")
		private List<DBRegistrationWrapUpConfig> wrapUpConfig = Collections.emptyList();
		@JsonProperty("PolicyAgreements")
		private List<DBPolicyAgreementConfiguration> policyAgreements = Collections.emptyList();
		@JsonProperty("ByInvitationOnly")
		private boolean byInvitationOnly;
		@JsonProperty("CheckIdentityOnSubmit")
		private boolean checkIdentityOnSubmit;

		protected DBBaseFormBuilder()
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
		public T withIdentityParams(List<DBIdentityRegistrationParam> identityParams)
		{
			this.identityParams = Optional.ofNullable(identityParams)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withAttributeParams(List<DBAttributeRegistrationParam> attributeParams)
		{
			this.attributeParams = Optional.ofNullable(attributeParams)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withGroupParams(List<DBGroupRegistrationParam> groupParams)
		{
			this.groupParams = Optional.ofNullable(groupParams)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withCredentialParams(List<DBCredentialRegistrationParam> credentialParams)
		{
			this.credentialParams = Optional.ofNullable(credentialParams)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withAgreements(List<DBAgreementRegistrationParam> agreements)
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
		public T withDisplayedName(DBI18nString displayedName)
		{
			this.displayedName = displayedName;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withFormInformation(DBI18nString formInformation)
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
		public T withPageTitle(DBI18nString pageTitle)
		{
			this.pageTitle = pageTitle;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withTranslationProfile(DBTranslationProfile translationProfile)
		{
			this.translationProfile = translationProfile;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withLayoutSettings(DBFormLayoutSettings layoutSettings)
		{
			this.layoutSettings = layoutSettings;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withWrapUpConfig(List<DBRegistrationWrapUpConfig> wrapUpConfig)
		{
			this.wrapUpConfig = Optional.ofNullable(wrapUpConfig)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withPolicyAgreements(List<DBPolicyAgreementConfiguration> policyAgreements)
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

		public DBBaseForm build()
		{
			return new DBBaseForm(this);
		}
	}

}
