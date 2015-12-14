/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.List;

import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.types.DescribedObjectImpl;
import pl.edu.icm.unity.types.I18nString;

/**
 * Configuration of a registration form. Registration form data contains:
 * <ol>
 *  <li> its identification and description,
 *  <li> visibility, which controls whether the form is publicly available for all (anonymous) clients or
 *  whether only for authorized administrators. 
 *  <li> configuration of what information is collected during registration (and in how),
 *  <li> extra information to be presented to the user
 *  <li> translation profile which can modify the data collected by the form
 * </ol>
 * @author K. Benedyczak
 */
public class RegistrationForm extends DescribedObjectImpl
{
	private boolean publiclyAvailable;
	private RegistrationFormNotifications notificationsConfiguration = new RegistrationFormNotifications();
	
	private List<IdentityRegistrationParam> identityParams;
	private List<AttributeRegistrationParam> attributeParams;	
	private List<GroupRegistrationParam> groupParams;
	private List<CredentialRegistrationParam> credentialParams;
	private List<AgreementRegistrationParam> agreements;
	private boolean collectComments;
	private int captchaLength;
	private I18nString displayedName;
	private I18nString formInformation;
	private String registrationCode;
	
	private String defaultCredentialRequirement;
	private RegistrationTranslationProfile translationProfile; 
		
	public RegistrationFormNotifications getNotificationsConfiguration()
	{
		return notificationsConfiguration;
	}

	public void setNotificationsConfiguration(RegistrationFormNotifications notificationsConfiguration)
	{
		this.notificationsConfiguration = notificationsConfiguration;
	}

	public List<IdentityRegistrationParam> getIdentityParams()
	{
		return identityParams;
	}

	public void setIdentityParams(List<IdentityRegistrationParam> identityParams)
	{
		this.identityParams = identityParams;
	}

	public List<AttributeRegistrationParam> getAttributeParams()
	{
		return attributeParams;
	}

	public void setAttributeParams(List<AttributeRegistrationParam> attributeParams)
	{
		this.attributeParams = attributeParams;
	}

	public List<GroupRegistrationParam> getGroupParams()
	{
		return groupParams;
	}

	public void setGroupParams(List<GroupRegistrationParam> groupParams)
	{
		this.groupParams = groupParams;
	}

	public List<CredentialRegistrationParam> getCredentialParams()
	{
		return credentialParams;
	}

	public void setCredentialParams(List<CredentialRegistrationParam> credentialParams)
	{
		this.credentialParams = credentialParams;
	}

	public List<AgreementRegistrationParam> getAgreements()
	{
		return agreements;
	}

	public void setAgreements(List<AgreementRegistrationParam> agreements)
	{
		this.agreements = agreements;
	}

	public boolean isCollectComments()
	{
		return collectComments;
	}

	public void setCollectComments(boolean collectComments)
	{
		this.collectComments = collectComments;
	}

	public I18nString getFormInformation()
	{
		return formInformation;
	}

	public void setFormInformation(I18nString formInformation)
	{
		this.formInformation = formInformation;
	}

	public I18nString getDisplayedName()
	{
		return displayedName == null ? new I18nString(getName()) : displayedName;
	}

	public void setDisplayedName(I18nString displayedName)
	{
		this.displayedName = displayedName;
	}

	public String getRegistrationCode()
	{
		return registrationCode;
	}

	public void setRegistrationCode(String registrationCode)
	{
		this.registrationCode = registrationCode;
	}

	public boolean isPubliclyAvailable()
	{
		return publiclyAvailable;
	}

	public void setPubliclyAvailable(boolean publiclyAvailable)
	{
		this.publiclyAvailable = publiclyAvailable;
	}


	public int getCaptchaLength()
	{
		return captchaLength;
	}

	public void setCaptchaLength(int captchaLength)
	{
		this.captchaLength = captchaLength;
	}

	@Override
	public void setName(String name)
	{
		super.setName(name);
		if (displayedName == null)
			displayedName = new I18nString(name);
	}

	public String getDefaultCredentialRequirement()
	{
		return defaultCredentialRequirement;
	}

	public void setDefaultCredentialRequirement(String defaultCredentialRequirement)
	{
		this.defaultCredentialRequirement = defaultCredentialRequirement;
	}

	public RegistrationTranslationProfile getTranslationProfile()
	{
		return translationProfile;
	}

	public void setTranslationProfile(RegistrationTranslationProfile translationProfile)
	{
		this.translationProfile = translationProfile;
	}
	
	
	public boolean containsAutomaticAndMandatoryParams()
	{
		if (identityParams != null)
		{
			for (IdentityRegistrationParam id : identityParams)
			{
				if (checkAutoParam(id))
					return true;
			}
		}
		if (attributeParams != null)
		{
			for (AttributeRegistrationParam at : attributeParams)
			{
				if (checkAutoParam(at))
					return true;
			}
		}
		if (groupParams != null)
		{
			for (GroupRegistrationParam gr : groupParams)
			{
				if (gr.getRetrievalSettings().isAutomaticOnly())
					return true;
			}
		}
		return false;
	}
	
	private boolean checkAutoParam(OptionalRegistrationParam param)
	{
		if (!param.isOptional() && (param.getRetrievalSettings().isAutomaticOnly()))
			return true;

		return false;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((agreements == null) ? 0 : agreements.hashCode());
		result = prime * result
				+ ((attributeParams == null) ? 0 : attributeParams.hashCode());
		result = prime * result + captchaLength;
		result = prime * result + (collectComments ? 1231 : 1237);
		result = prime * result
				+ ((credentialParams == null) ? 0 : credentialParams.hashCode());
		result = prime
				* result
				+ ((defaultCredentialRequirement == null) ? 0
						: defaultCredentialRequirement.hashCode());
		result = prime * result + ((displayedName == null) ? 0 : displayedName.hashCode());
		result = prime * result
				+ ((formInformation == null) ? 0 : formInformation.hashCode());
		result = prime * result + ((groupParams == null) ? 0 : groupParams.hashCode());
		result = prime * result
				+ ((identityParams == null) ? 0 : identityParams.hashCode());
		result = prime
				* result
				+ ((notificationsConfiguration == null) ? 0
						: notificationsConfiguration.hashCode());
		result = prime * result + (publiclyAvailable ? 1231 : 1237);
		result = prime * result
				+ ((registrationCode == null) ? 0 : registrationCode.hashCode());
		result = prime
				* result
				+ ((translationProfile == null) ? 0 : translationProfile.hashCode());
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
		RegistrationForm other = (RegistrationForm) obj;
		if (agreements == null)
		{
			if (other.agreements != null)
				return false;
		} else if (!agreements.equals(other.agreements))
			return false;
		if (attributeParams == null)
		{
			if (other.attributeParams != null)
				return false;
		} else if (!attributeParams.equals(other.attributeParams))
			return false;
		if (captchaLength != other.captchaLength)
			return false;
		if (collectComments != other.collectComments)
			return false;
		if (credentialParams == null)
		{
			if (other.credentialParams != null)
				return false;
		} else if (!credentialParams.equals(other.credentialParams))
			return false;
		if (defaultCredentialRequirement == null)
		{
			if (other.defaultCredentialRequirement != null)
				return false;
		} else if (!defaultCredentialRequirement.equals(other.defaultCredentialRequirement))
			return false;
		if (displayedName == null)
		{
			if (other.displayedName != null)
				return false;
		} else if (!displayedName.equals(other.displayedName))
			return false;
		if (formInformation == null)
		{
			if (other.formInformation != null)
				return false;
		} else if (!formInformation.equals(other.formInformation))
			return false;
		if (groupParams == null)
		{
			if (other.groupParams != null)
				return false;
		} else if (!groupParams.equals(other.groupParams))
			return false;
		if (identityParams == null)
		{
			if (other.identityParams != null)
				return false;
		} else if (!identityParams.equals(other.identityParams))
			return false;
		if (notificationsConfiguration == null)
		{
			if (other.notificationsConfiguration != null)
				return false;
		} else if (!notificationsConfiguration.equals(other.notificationsConfiguration))
			return false;
		if (publiclyAvailable != other.publiclyAvailable)
			return false;
		if (registrationCode == null)
		{
			if (other.registrationCode != null)
				return false;
		} else if (!registrationCode.equals(other.registrationCode))
			return false;
		if (translationProfile == null)
		{
			if (other.translationProfile != null)
				return false;
		} else if (!translationProfile.equals(other.translationProfile))
			return false;
		return true;
	}
}
