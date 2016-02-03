/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.DescribedObjectImpl;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
	
	private List<IdentityRegistrationParam> identityParams = new ArrayList<>();
	private List<AttributeRegistrationParam> attributeParams = new ArrayList<>();	
	private List<GroupRegistrationParam> groupParams = new ArrayList<>();
	private List<CredentialRegistrationParam> credentialParams = new ArrayList<>();
	private List<AgreementRegistrationParam> agreements = new ArrayList<>();
	private boolean collectComments;
	private int captchaLength;
	private I18nString displayedName = new I18nString();
	private I18nString formInformation = new I18nString();
	private String registrationCode;
	
	private String defaultCredentialRequirement;
	private TranslationProfile translationProfile = 
			new TranslationProfile("registrationProfile", "", ProfileType.REGISTRATION, new ArrayList<>()); 

	@JsonCreator
	public RegistrationForm(ObjectNode json)
	{
		fromJson(json);
	}
	
	public RegistrationForm()
	{
	}
	
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

	public TranslationProfile getTranslationProfile()
	{
		return translationProfile;
	}

	public void setTranslationProfile(TranslationProfile translationProfile)
	{
		if (translationProfile.getProfileType() != ProfileType.REGISTRATION)
			throw new IllegalArgumentException("Only a registration profile can be used with registration form");
		this.translationProfile = translationProfile;
	}
	
	@Override
	public String toString()
	{
		return "RegistrationForm [name=" + name + "]";
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

	public void validate()
	{
		if (identityParams == null || groupParams == null || agreements == null 
				|| credentialParams == null || attributeParams == null)
			throw new IllegalStateException("All lists must be not-null in RegistrationForm");
		if (defaultCredentialRequirement == null)
			throw new IllegalStateException("Default credential requirement must be not-null "
					+ "in RegistrationForm");
		if (translationProfile == null)
			throw new IllegalStateException("Translation profile must be not-null "
					+ "in RegistrationForm");
		if (name == null)
			throw new IllegalStateException("Name must be not-null "
					+ "in RegistrationForm");
		if (displayedName == null)
			throw new IllegalStateException("Displayed name must be not-null "
					+ "in RegistrationForm (but it contents can be empty)");
		if (formInformation == null)
			throw new IllegalStateException("Form information must be not-null "
					+ "in RegistrationForm (but it contents can be empty)");
	}
	
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		ObjectNode root = jsonMapper.createObjectNode();
		root.set("Agreements", serializeAgreements(jsonMapper, getAgreements()));
		root.set("AttributeParams", jsonMapper.valueToTree(getAttributeParams()));
		root.put("CollectComments", isCollectComments());
		root.set("CredentialParams", jsonMapper.valueToTree(getCredentialParams()));
		root.put("DefaultCredentialRequirement", getDefaultCredentialRequirement());
		root.put("Description", getDescription());
		root.set("i18nFormInformation", I18nStringJsonUtil.toJson(getFormInformation()));
		root.set("GroupParams", jsonMapper.valueToTree(getGroupParams()));
		root.set("IdentityParams", jsonMapper.valueToTree(getIdentityParams()));
		root.put("Name", getName());
		root.set("DisplayedName", I18nStringJsonUtil.toJson(getDisplayedName()));
		root.set("NotificationsConfiguration", jsonMapper.valueToTree(getNotificationsConfiguration()));
		root.put("PubliclyAvailable", isPubliclyAvailable());
		root.put("RegistrationCode", getRegistrationCode());
		root.put("CaptchaLength", getCaptchaLength());
		root.set("TranslationProfile", getTranslationProfile().toJsonObject());
		return root;
	}

	private JsonNode serializeAgreements(ObjectMapper jsonMapper, List<AgreementRegistrationParam> agreements)
	{
		ArrayNode root = jsonMapper.createArrayNode();
		for (AgreementRegistrationParam agreement: agreements)
		{
			ObjectNode node = root.addObject();
			node.set("i18nText", I18nStringJsonUtil.toJson(agreement.getText()));
			node.put("manatory", agreement.isManatory());
		}
		return root;
	}

	private List<AgreementRegistrationParam> loadAgreements(ArrayNode root)
	{
		List<AgreementRegistrationParam> ret = new ArrayList<AgreementRegistrationParam>();
		
		for (JsonNode nodeR: root)
		{
			ObjectNode node = (ObjectNode) nodeR;
			AgreementRegistrationParam param = new AgreementRegistrationParam();
			ret.add(param);
			
			param.setText(I18nStringJsonUtil.fromJson(node.get("i18nText"), node.get("text")));
			param.setManatory(node.get("manatory").asBoolean());
		}
		
		return ret;
	}
	
	private void fromJson(ObjectNode root)
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		try
		{
			JsonNode n = root.get("Agreements");
			if (n != null)
			{
				setAgreements(loadAgreements((ArrayNode) n));
			}
			
			n = root.get("AttributeParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<AttributeRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<AttributeRegistrationParam>>(){});
				setAttributeParams(r);
			}
			n = root.get("CollectComments");
			setCollectComments(n.asBoolean());
			n = root.get("CredentialParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<CredentialRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<CredentialRegistrationParam>>(){});
				setCredentialParams(r);
			}
			n = root.get("DefaultCredentialRequirement");
			setDefaultCredentialRequirement(n == null ? null : n.asText());
			n = root.get("Description");
			setDescription((n == null || n.isNull()) ? null : n.asText());
			
			setFormInformation(I18nStringJsonUtil.fromJson(root.get("i18nFormInformation"), 
					root.get("FormInformation")));
			
			n = root.get("GroupParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<GroupRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<GroupRegistrationParam>>(){});
				setGroupParams(r);
			}

			n = root.get("IdentityParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<IdentityRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<IdentityRegistrationParam>>(){});
				setIdentityParams(r);
			}

			n = root.get("Name");
			setName(n.asText());
			
			if (root.has("DisplayedName"))
				setDisplayedName(I18nStringJsonUtil.fromJson(root.get("DisplayedName")));
			
			n = root.get("NotificationsConfiguration");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				RegistrationFormNotifications r = jsonMapper.readValue(v, 
						new TypeReference<RegistrationFormNotifications>(){});
				setNotificationsConfiguration(r);
			}

			n = root.get("PubliclyAvailable");
			setPubliclyAvailable(n.asBoolean());
			n = root.get("RegistrationCode");
			setRegistrationCode((n == null || n.isNull()) ? null : n.asText());
			
			if (root.has("CaptchaLength"))
			{
				n = root.get("CaptchaLength");
				setCaptchaLength(n.asInt());
			} else
			{
				setCaptchaLength(0);
			}

			n = root.get("TranslationProfile");
			if (n != null)
			{
				setTranslationProfile(new TranslationProfile((ObjectNode) n));
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize registration form from JSON", e);
		}
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
