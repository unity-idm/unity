/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.objstore.reg.common.DBBaseForm;
import pl.edu.icm.unity.store.types.common.DBI18nString;

@JsonDeserialize(builder = DBRegistrationForm.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
class DBRegistrationForm extends DBBaseForm
{
	@JsonProperty("PubliclyAvailable")
	public final boolean publiclyAvailable;
	@JsonProperty("NotificationsConfiguration")
	public final DBRegistrationFormNotifications notificationsConfiguration;
	@JsonProperty("CaptchaLength")
	public final int captchaLength;
	@JsonProperty("RegistrationCode")
	public final String registrationCode;
	@JsonProperty("DefaultCredentialRequirement")
	public final String defaultCredentialRequirement;
	@JsonProperty("Title2ndStage")
	public final DBI18nString title2ndStage;
	@JsonProperty("ExternalSignupSpec")
	public final DBExternalSignupSpec externalSignupSpec;
	@JsonProperty("ExternalSignupGridSpec")
	public final DBExternalSignupGridSpec externalSignupGridSpec;
	@JsonProperty("RegistrationFormLayouts")
	public final DBRegistrationFormLayouts formLayouts;
	@JsonProperty("ShowSignInLink")
	public final boolean showSignInLink;
	@JsonProperty("SignInLink")
	public final String signInLink;
	@JsonProperty("SwitchToEnquiryInfo")
	public final DBI18nString switchToEnquiryInfo;
	@JsonProperty("AutoLoginToRealm")
	public final String autoLoginToRealm;
	@JsonProperty("FormInformation2ndStage")
	public final DBI18nString formInformation2ndStage;

	private DBRegistrationForm(Builder builder)
	{
		super(builder);
		this.publiclyAvailable = builder.publiclyAvailable;
		this.notificationsConfiguration = builder.notificationsConfiguration;
		this.captchaLength = builder.captchaLength;
		this.registrationCode = builder.registrationCode;
		this.defaultCredentialRequirement = builder.defaultCredentialRequirement;
		this.title2ndStage = builder.title2ndStage;
		this.externalSignupSpec = builder.externalSignupSpec;
		this.externalSignupGridSpec = builder.externalSignupGridSpec;
		this.formLayouts = builder.formLayouts;
		this.showSignInLink = builder.showSignInLink;
		this.signInLink = builder.signInLink;
		this.switchToEnquiryInfo = builder.switchToEnquiryInfo;
		this.autoLoginToRealm = builder.autoLoginToRealm;
		this.formInformation2ndStage = builder.formInformation2ndStage;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(autoLoginToRealm, captchaLength, defaultCredentialRequirement,
				externalSignupGridSpec, externalSignupSpec, formLayouts, notificationsConfiguration, publiclyAvailable,
				registrationCode, showSignInLink, signInLink, switchToEnquiryInfo, title2ndStage, formInformation2ndStage);
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
		DBRegistrationForm other = (DBRegistrationForm) obj;
		return Objects.equals(autoLoginToRealm, other.autoLoginToRealm) && captchaLength == other.captchaLength
				&& Objects.equals(defaultCredentialRequirement, other.defaultCredentialRequirement)
				&& Objects.equals(externalSignupGridSpec, other.externalSignupGridSpec)
				&& Objects.equals(externalSignupSpec, other.externalSignupSpec)
				&& Objects.equals(formLayouts, other.formLayouts)
				&& Objects.equals(notificationsConfiguration, other.notificationsConfiguration)
				&& publiclyAvailable == other.publiclyAvailable
				&& Objects.equals(registrationCode, other.registrationCode) && showSignInLink == other.showSignInLink
				&& Objects.equals(signInLink, other.signInLink)
				&& Objects.equals(switchToEnquiryInfo, other.switchToEnquiryInfo)
				&& Objects.equals(title2ndStage, other.title2ndStage)
				&& Objects.equals(formInformation2ndStage, other.formInformation2ndStage);
	}

	public static Builder builder()
	{
		return new Builder();
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder extends DBBaseFormBuilder<Builder>
	{
		@JsonProperty("PubliclyAvailable")
		private boolean publiclyAvailable;
		@JsonProperty("NotificationsConfiguration")
		private DBRegistrationFormNotifications notificationsConfiguration = DBRegistrationFormNotifications
				.builder()
				.build();
		@JsonProperty("CaptchaLength")
		private int captchaLength;
		@JsonProperty("RegistrationCode")
		private String registrationCode;
		@JsonProperty("DefaultCredentialRequirement")
		private String defaultCredentialRequirement;
		@JsonProperty("Title2ndStage")
		private DBI18nString title2ndStage = DBI18nString.builder()
				.build();
		@JsonProperty("ExternalSignupSpec")
		private DBExternalSignupSpec externalSignupSpec = DBExternalSignupSpec.builder()
				.build();
		@JsonProperty("ExternalSignupGridSpec")
		private DBExternalSignupGridSpec externalSignupGridSpec = DBExternalSignupGridSpec.builder()
				.build();
		@JsonProperty("RegistrationFormLayouts")
		private DBRegistrationFormLayouts formLayouts = DBRegistrationFormLayouts.builder()
				.build();
		@JsonProperty("ShowSignInLink")
		private boolean showSignInLink;
		@JsonProperty("SignInLink")
		private String signInLink;
		@JsonProperty("SwitchToEnquiryInfo")
		private DBI18nString switchToEnquiryInfo;
		@JsonProperty("AutoLoginToRealm")
		private String autoLoginToRealm;
		@JsonProperty("FormInformation2ndStage")
		private DBI18nString formInformation2ndStage;

		private Builder()
		{
		}

		public Builder withPubliclyAvailable(boolean publiclyAvailable)
		{
			this.publiclyAvailable = publiclyAvailable;
			return this;
		}

		public Builder withNotificationsConfiguration(DBRegistrationFormNotifications notificationsConfiguration)
		{
			this.notificationsConfiguration = notificationsConfiguration;
			return this;
		}

		public Builder withCaptchaLength(int captchaLength)
		{
			this.captchaLength = captchaLength;
			return this;
		}

		public Builder withRegistrationCode(String registrationCode)
		{
			this.registrationCode = registrationCode;
			return this;
		}

		public Builder withDefaultCredentialRequirement(String defaultCredentialRequirement)
		{
			this.defaultCredentialRequirement = defaultCredentialRequirement;
			return this;
		}

		public Builder withTitle2ndStage(DBI18nString title2ndStage)
		{
			this.title2ndStage = title2ndStage;
			return this;
		}
		
		public Builder withFormInformation2ndStage(DBI18nString formInformation2ndStage)
		{
			this.formInformation2ndStage = formInformation2ndStage;
			return this;
		}

		public Builder withExternalSignupSpec(DBExternalSignupSpec externalSignupSpec)
		{
			this.externalSignupSpec = externalSignupSpec;
			return this;
		}

		public Builder withExternalSignupGridSpec(DBExternalSignupGridSpec externalSignupGridSpec)
		{
			this.externalSignupGridSpec = externalSignupGridSpec;
			return this;
		}

		public Builder withFormLayouts(DBRegistrationFormLayouts formLayouts)
		{
			this.formLayouts = formLayouts;
			return this;
		}

		public Builder withShowSignInLink(boolean showSignInLink)
		{
			this.showSignInLink = showSignInLink;
			return this;
		}

		public Builder withSignInLink(String signInLink)
		{
			this.signInLink = signInLink;
			return this;
		}

		public Builder withSwitchToEnquiryInfo(DBI18nString switchToEnquiryInfo)
		{
			this.switchToEnquiryInfo = switchToEnquiryInfo;
			return this;
		}

		public Builder withAutoLoginToRealm(String autoLoginToRealm)
		{
			this.autoLoginToRealm = autoLoginToRealm;
			return this;
		}

		public DBRegistrationForm build()
		{
			return new DBRegistrationForm(this);
		}
	}

}
