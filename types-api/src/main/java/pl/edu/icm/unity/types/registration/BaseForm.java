/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.DescribedObjectROImpl;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;


/**
 * Base class with shared state for {@link RegistrationForm} and {@link EnquiryForm}.
 *  
 *  
 * @author K. Benedyczak
 */
public abstract class BaseForm extends DescribedObjectROImpl
{
	private List<IdentityRegistrationParam> identityParams = new ArrayList<>();
	private List<AttributeRegistrationParam> attributeParams = new ArrayList<>();	
	private List<GroupRegistrationParam> groupParams = new ArrayList<>();
	private List<CredentialRegistrationParam> credentialParams = new ArrayList<>();
	private List<AgreementRegistrationParam> agreements = new ArrayList<>();
	private boolean collectComments;
	private I18nString displayedName = new I18nString();
	private I18nString formInformation = new I18nString();
	private I18nString pageTitle = new I18nString();
	private TranslationProfile translationProfile = 
			new TranslationProfile("registrationProfile", "", ProfileType.REGISTRATION, new ArrayList<>());
	private FormLayoutSettings layoutSettings = FormLayoutSettings.DEFAULT;
	private List<RegistrationWrapUpConfig> wrapUpConfig = new ArrayList<>();
	private boolean byInvitationOnly;
	
	@JsonCreator
	BaseForm(ObjectNode json)
	{
		fromJson(json);
		validate();
	}
	
	BaseForm()
	{
	}

	protected final void validate()
	{
		if (identityParams == null || groupParams == null || agreements == null 
				|| credentialParams == null || attributeParams == null)
			throw new IllegalStateException("All lists must be not-null in a form");
		if (translationProfile == null)
			throw new IllegalStateException("Translation profile must be not-null "
					+ "in a form");
		if (name == null)
			throw new IllegalStateException("Name must be not-null "
					+ "in a form");
		if (displayedName == null)
			throw new IllegalStateException("Displayed name must be not-null "
					+ "in a form (but it contents can be empty)");
		if (formInformation == null)
			throw new IllegalStateException("Form information must be not-null "
					+ "in a form (but it contents can be empty)");
	}
	
	@Override
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		ObjectNode root = jsonMapper.createObjectNode();
		root.set("Agreements", serializeAgreements(jsonMapper, getAgreements()));
		root.set("AttributeParams", jsonMapper.valueToTree(getAttributeParams()));
		root.put("CollectComments", isCollectComments());
		root.set("CredentialParams", jsonMapper.valueToTree(getCredentialParams()));
		root.put("Description", getDescription());
		root.set("i18nFormInformation", I18nStringJsonUtil.toJson(getFormInformation()));
		root.set("GroupParams", jsonMapper.valueToTree(getGroupParams()));
		root.set("IdentityParams", jsonMapper.valueToTree(getIdentityParams()));
		root.put("Name", getName());
		root.set("DisplayedName", I18nStringJsonUtil.toJson(getDisplayedName()));
		root.set("TranslationProfile", getTranslationProfile().toJsonObject());
		root.set("FormLayoutSettings", jsonMapper.valueToTree(getLayoutSettings()));
		root.set("PageTitle", jsonMapper.valueToTree(getPageTitle()));
		root.set("WrapUpConfig", jsonMapper.valueToTree(getWrapUpConfig()));
		root.put("ByInvitationOnly", isByInvitationOnly());
		return root;
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
			
			n = root.get("TranslationProfile");
			if (n != null)
			{
				setTranslationProfile(new TranslationProfile((ObjectNode) n));
			}
			
			n = root.get("FormLayoutSettings");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				FormLayoutSettings r = jsonMapper.readValue(v, 
						new TypeReference<FormLayoutSettings>(){});
				setLayoutSettings(r);
			}
			
			if (JsonUtil.notNull(root, "PageTitle"))
			{
				setPageTitle(I18nStringJsonUtil.fromJson(root.get("PageTitle")));
			}

			n = root.get("WrapUpConfig");
			if (n != null && !n.isNull())
				setWrapUpConfig(jsonMapper.convertValue(n, new TypeReference<List<RegistrationWrapUpConfig>>(){}));
			
			n = root.get("ByInvitationOnly");
			if (n != null && !n.isNull())
				setByInvitationOnly(n.asBoolean());
			
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize a form from JSON", e);
		}
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
		List<AgreementRegistrationParam> ret = new ArrayList<>();
		
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
	
	void setName(String name)
	{
		this.name = name;
		if (displayedName == null)
			displayedName = new I18nString(name);
	}
	
	void setDescription(String description)
	{
		this.description = description;
	}
	
	public List<IdentityRegistrationParam> getIdentityParams()
	{
		return identityParams;
	}

	void setIdentityParams(List<IdentityRegistrationParam> identityParams)
	{
		this.identityParams = identityParams;
	}

	public List<AttributeRegistrationParam> getAttributeParams()
	{
		return attributeParams;
	}

	void setAttributeParams(List<AttributeRegistrationParam> attributeParams)
	{
		this.attributeParams = attributeParams;
	}

	public List<GroupRegistrationParam> getGroupParams()
	{
		return groupParams;
	}

	void setGroupParams(List<GroupRegistrationParam> groupParams)
	{
		this.groupParams = groupParams;
	}

	public List<CredentialRegistrationParam> getCredentialParams()
	{
		return credentialParams;
	}

	void setCredentialParams(List<CredentialRegistrationParam> credentialParams)
	{
		this.credentialParams = credentialParams;
	}

	public List<AgreementRegistrationParam> getAgreements()
	{
		return agreements;
	}

	void setAgreements(List<AgreementRegistrationParam> agreements)
	{
		this.agreements = agreements;
	}

	public boolean isCollectComments()
	{
		return collectComments;
	}

	void setCollectComments(boolean collectComments)
	{
		this.collectComments = collectComments;
	}
	public I18nString getFormInformation()
	{
		return formInformation;
	}

	void setFormInformation(I18nString formInformation)
	{
		this.formInformation = formInformation;
	}

	public I18nString getDisplayedName()
	{
		return displayedName == null ? new I18nString(getName()) : displayedName;
	}

	void setDisplayedName(I18nString displayedName)
	{
		this.displayedName = displayedName;
	}
	
	public TranslationProfile getTranslationProfile()
	{
		return translationProfile;
	}
	
	public FormLayoutSettings getLayoutSettings()
	{
		return layoutSettings;
	}

	public void setLayoutSettings(FormLayoutSettings layoutSettings)
	{
		this.layoutSettings = layoutSettings;
	}
	
	public I18nString getPageTitle()
	{
		return pageTitle;
	}

	public void setPageTitle(I18nString pageTitle)
	{
		this.pageTitle = pageTitle;
	}

	void setTranslationProfile(TranslationProfile translationProfile)
	{
		if (translationProfile.getProfileType() != ProfileType.REGISTRATION)
			throw new IllegalArgumentException("Only a registration profile can be used with registration form");
		this.translationProfile = translationProfile;
	}

	public List<RegistrationWrapUpConfig> getWrapUpConfig()
	{
		return wrapUpConfig;
	}

	public void setWrapUpConfig(List<RegistrationWrapUpConfig> wrapUpConfig)
	{
		this.wrapUpConfig = wrapUpConfig;
	}
	
	public boolean isByInvitationOnly()
	{
		return byInvitationOnly;
	}

	public void setByInvitationOnly(boolean byInvitationOnly)
	{
		this.byInvitationOnly = byInvitationOnly;
	}

	public boolean isLocalSignupEnabled()
	{
		return !credentialParams.isEmpty();
	}
	
	public abstract BaseFormNotifications getNotificationsConfiguration();
	
	@Override
	public boolean equals(final Object other)
	{
		if (this == other)
			return true;
		if (!(other instanceof BaseForm))
			return false;
		if (!super.equals(other))
			return false;
		BaseForm castOther = (BaseForm) other;
		return Objects.equals(identityParams, castOther.identityParams)
				&& Objects.equals(attributeParams, castOther.attributeParams)
				&& Objects.equals(groupParams, castOther.groupParams)
				&& Objects.equals(credentialParams, castOther.credentialParams)
				&& Objects.equals(agreements, castOther.agreements)
				&& Objects.equals(collectComments, castOther.collectComments)
				&& Objects.equals(displayedName, castOther.displayedName)
				&& Objects.equals(formInformation, castOther.formInformation)
				&& Objects.equals(translationProfile, castOther.translationProfile)
				&& Objects.equals(layoutSettings, castOther.layoutSettings)
				&& Objects.equals(wrapUpConfig, castOther.wrapUpConfig)
				&& Objects.equals(byInvitationOnly, castOther.byInvitationOnly);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), identityParams, attributeParams, groupParams, credentialParams,
				agreements, collectComments, displayedName, formInformation, translationProfile, 
				layoutSettings, wrapUpConfig, byInvitationOnly);
	}
}
