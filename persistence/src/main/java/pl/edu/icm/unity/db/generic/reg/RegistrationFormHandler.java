/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.utils.I18nStringJsonUtil;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handler for {@link RegistrationForm}
 * @author K. Benedyczak
 */
@Component
public class RegistrationFormHandler extends DefaultEntityHandler<RegistrationForm>
{
	public static final String REGISTRATION_FORM_OBJECT_TYPE = "registrationForm";
	private TranslationActionsRegistry translationActionsRegistry;
	
	@Autowired
	public RegistrationFormHandler(ObjectMapper jsonMapper, 
			TranslationActionsRegistry translationActionsRegistry)
	{
		super(jsonMapper, REGISTRATION_FORM_OBJECT_TYPE, RegistrationForm.class);
		this.translationActionsRegistry = translationActionsRegistry;
	}

	@Override
	public GenericObjectBean toBlob(RegistrationForm value, SqlSession sql)
	{
		try
		{
			ObjectNode root = jsonMapper.createObjectNode();
			root.set("Agreements", serializeAgreements(value.getAgreements()));
			root.set("AttributeParams", jsonMapper.valueToTree(value.getAttributeParams()));
			root.put("CollectComments", value.isCollectComments());
			root.set("CredentialParams", jsonMapper.valueToTree(value.getCredentialParams()));
			root.put("DefaultCredentialRequirement", value.getDefaultCredentialRequirement());
			root.put("Description", value.getDescription());
			root.set("i18nFormInformation", I18nStringJsonUtil.toJson(value.getFormInformation()));
			root.set("GroupParams", jsonMapper.valueToTree(value.getGroupParams()));
			root.set("IdentityParams", jsonMapper.valueToTree(value.getIdentityParams()));
			root.put("Name", value.getName());
			root.set("DisplayedName", I18nStringJsonUtil.toJson(value.getDisplayedName()));
			root.set("NotificationsConfiguration", jsonMapper.valueToTree(value.getNotificationsConfiguration()));
			root.put("PubliclyAvailable", value.isPubliclyAvailable());
			root.put("RegistrationCode", value.getRegistrationCode());
			root.put("CaptchaLength", value.getCaptchaLength());
			root.set("TranslationProfile", value.getTranslationProfile().toJsonObject(jsonMapper));
			byte[] contents = jsonMapper.writeValueAsBytes(root);
			return new GenericObjectBean(value.getName(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize registration form to JSON", e);
		}
	}

	private JsonNode serializeAgreements(List<AgreementRegistrationParam> agreements)
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
	
	@Override
	public RegistrationForm fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			ObjectNode root = (ObjectNode) jsonMapper.readTree(blob.getContents());
			RegistrationForm ret = new RegistrationForm();
			
			JsonNode n = root.get("Agreements");
			if (n != null)
			{
				ret.setAgreements(loadAgreements((ArrayNode) n));
			}
			
			n = root.get("AttributeParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<AttributeRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<AttributeRegistrationParam>>(){});
				ret.setAttributeParams(r);
			}
			n = root.get("CollectComments");
			ret.setCollectComments(n.asBoolean());
			n = root.get("CredentialParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<CredentialRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<CredentialRegistrationParam>>(){});
				ret.setCredentialParams(r);
			}
			n = root.get("DefaultCredentialRequirement");
			ret.setDefaultCredentialRequirement(n == null ? null : n.asText());
			n = root.get("Description");
			ret.setDescription(n == null ? null : n.asText());
			
			ret.setFormInformation(I18nStringJsonUtil.fromJson(root.get("i18nFormInformation"), 
					root.get("FormInformation")));
			
			n = root.get("GroupParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<GroupRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<GroupRegistrationParam>>(){});
				ret.setGroupParams(r);
			}

			n = root.get("IdentityParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<IdentityRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<IdentityRegistrationParam>>(){});
				ret.setIdentityParams(r);
			}

			n = root.get("Name");
			ret.setName(n.asText());
			
			if (root.has("DisplayedName"))
				ret.setDisplayedName(I18nStringJsonUtil.fromJson(root.get("DisplayedName")));
			
			n = root.get("NotificationsConfiguration");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				RegistrationFormNotifications r = jsonMapper.readValue(v, 
						new TypeReference<RegistrationFormNotifications>(){});
				ret.setNotificationsConfiguration(r);
			}

			n = root.get("PubliclyAvailable");
			ret.setPubliclyAvailable(n.asBoolean());
			n = root.get("RegistrationCode");
			ret.setRegistrationCode((n == null || n.isNull()) ? null : n.asText());
			
			if (root.has("CaptchaLength"))
			{
				n = root.get("CaptchaLength");
				ret.setCaptchaLength(n.asInt());
			} else
			{
				ret.setCaptchaLength(0);
			}

			n = root.get("TranslationProfile");
			if (n != null)
			{
				ret.setTranslationProfile(new RegistrationTranslationProfile((ObjectNode) n, 
						translationActionsRegistry));
			}
			
			return ret;
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize registration form from JSON", e);
		}
	}
}
