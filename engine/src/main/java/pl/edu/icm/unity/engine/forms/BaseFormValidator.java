/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Validation of {@link BaseForm}, useful as a base of {@link EnquiryForm} and {@link RegistrationForm}
 * validation.
 * @author K. Benedyczak
 */
@Component
public class BaseFormValidator
{
	private CredentialRepository credentialRepository;
	private AttributeTypeDAO attributeDAO;
	private MessageTemplateDB msgTplDB;
	private GroupDAO groupDAO;
	private IdentityTypesRegistry identityTypesRegistry;

	@Autowired
	public BaseFormValidator(CredentialRepository credentialRepository, AttributeTypeDAO attributeDAO,
			MessageTemplateDB msgTplDB, GroupDAO groupDAO,
			IdentityTypesRegistry identityTypesRegistry)
	{
		this.credentialRepository = credentialRepository;
		this.attributeDAO = attributeDAO;
		this.msgTplDB = msgTplDB;
		this.groupDAO = groupDAO;
		this.identityTypesRegistry = identityTypesRegistry;
	}



	public void validateBaseFormContents(BaseForm form) throws EngineException
	{
		Map<String, AttributeType> atMap = attributeDAO.getAllAsMap();

		if (form.getName() == null)
			throw new IllegalArgumentException("Form name is not set.");
		
		if (form.getTranslationProfile() == null)
			throw new IllegalArgumentException("Translation profile is not set.");
		
		if (form.getAttributeParams() != null)
		{
			Set<String> used = new HashSet<>();
			for (AttributeRegistrationParam attr: form.getAttributeParams())
			{
				if (!atMap.containsKey(attr.getAttributeType()))
					throw new IllegalArgumentException("Attribute type " + attr.getAttributeType() + 
							" does not exist");
				String key = attr.getAttributeType() + " @ " + attr.getGroup();
				if (used.contains(key))
					throw new IllegalArgumentException("Collected attribute " + key + 
							" was specified more then once.");
				used.add(key);
				if (!attr.isUsingDynamicGroup())
					groupDAO.get(attr.getGroup());
			}
		}

		if (form.getCredentialParams() != null)
		{
			Set<String> creds = new HashSet<>();
			for (CredentialRegistrationParam cred: form.getCredentialParams())
			{
				if (creds.contains(cred.getCredentialName()))
					throw new IllegalArgumentException("Collected credential " + 
							cred.getCredentialName() + " was specified more then once.");
				creds.add(cred.getCredentialName());
			}
			credentialRepository.assertExist(creds);
		}

		if (form.getGroupParams() != null)
		{
			Set<String> used = new HashSet<>();
			for (GroupRegistrationParam group: form.getGroupParams())
			{
				if (!GroupPatternMatcher.isValidPattern(group.getGroupPath()))
					throw new IllegalArgumentException(group.getGroupPath() +  
							" is not a valid group wildcard: must start with '/'");
				if (used.contains(group.getGroupPath()))
					throw new IllegalArgumentException("Selectable group " + group.getGroupPath() + 
							" was specified more then once.");
				used.add(group.getGroupPath());
			}
		}

		if (form.getIdentityParams() != null)
		{
			Set<String> usedRemote = new HashSet<>();
			for (IdentityRegistrationParam id: form.getIdentityParams())
			{
				identityTypesRegistry.getByName(id.getIdentityType());
				if (id.getRetrievalSettings() == ParameterRetrievalSettings.automatic || 
						id.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden)
				{
					if (usedRemote.contains(id.getIdentityType()))
						throw new IllegalArgumentException("There can be only one identity " +
								"collected automatically of each type. There are more " +
								"then one of type " + id.getIdentityType());
					usedRemote.add(id.getIdentityType());
				}
			}
		}
		
		if (form.getAgreements() != null)
		{
			for (AgreementRegistrationParam o: form.getAgreements())
			{
				if (o.getText() == null || o.getText().isEmpty())
					throw new IllegalArgumentException("Agreement text must not be empty.");
			}
		}
	}
	
	
	
	public void checkTemplate(String tpl, String compatibleDef, String purpose) throws EngineException
	{
		if (tpl != null)
		{
			if (!msgTplDB.exists(tpl))
				throw new IllegalArgumentException("Form has an unknown message template '" + tpl + "'");
			if (!compatibleDef.equals(msgTplDB.get(tpl).getConsumer()))
				throw new IllegalArgumentException("Template '" + tpl + 
						"' is not suitable as the " + purpose + " template");
		}
	}
}
