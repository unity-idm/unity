/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.registration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.generic.cred.CredentialDB;
import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateDB;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
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
	private CredentialDB credentialDB;
	private DBAttributes dbAttributes;
	private MessageTemplateDB msgTplDB;
	private GroupResolver groupsResolver;
	private IdentityTypesRegistry identityTypesRegistry;

	@Autowired
	public BaseFormValidator(CredentialDB credentialDB, DBAttributes dbAttributes,
			MessageTemplateDB msgTplDB, GroupResolver groupsResolver,
			IdentityTypesRegistry identityTypesRegistry)
	{
		this.credentialDB = credentialDB;
		this.dbAttributes = dbAttributes;
		this.msgTplDB = msgTplDB;
		this.groupsResolver = groupsResolver;
		this.identityTypesRegistry = identityTypesRegistry;
	}



	public void validateBaseFormContents(BaseForm form, SqlSession sql) throws EngineException
	{
		GroupsMapper gm = sql.getMapper(GroupsMapper.class);

		Map<String, AttributeType> atMap = dbAttributes.getAttributeTypes(sql);

		if (form.getTranslationProfile() == null)
			throw new WrongArgumentException("Translation profile is not set.");
		
		if (form.getAttributeParams() != null)
		{
			Set<String> used = new HashSet<>();
			for (AttributeRegistrationParam attr: form.getAttributeParams())
			{
				if (!atMap.containsKey(attr.getAttributeType()))
					throw new WrongArgumentException("Attribute type " + attr.getAttributeType() + 
							" does not exist");
				String key = attr.getAttributeType() + " @ " + attr.getGroup();
				if (used.contains(key))
					throw new WrongArgumentException("Collected attribute " + key + 
							" was specified more then once.");
				used.add(key);
				groupsResolver.resolveGroup(attr.getGroup(), gm);
			}
		}

		if (form.getCredentialParams() != null)
		{
			Set<String> creds = new HashSet<>();
			for (CredentialRegistrationParam cred: form.getCredentialParams())
			{
				if (creds.contains(cred.getCredentialName()))
					throw new WrongArgumentException("Collected credential " + 
							cred.getCredentialName() + " was specified more then once.");
				creds.add(cred.getCredentialName());
			}
			credentialDB.assertExist(creds, sql);
		}

		if (form.getGroupParams() != null)
		{
			Set<String> used = new HashSet<>();
			for (GroupRegistrationParam group: form.getGroupParams())
			{
				groupsResolver.resolveGroup(group.getGroupPath(), gm);
				if (used.contains(group.getGroupPath()))
					throw new WrongArgumentException("Selectable group " + group.getGroupPath() + 
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
						throw new WrongArgumentException("There can be only one identity " +
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
					throw new WrongArgumentException("Agreement text must not be empty.");
			}
		}
	}
	
	
	
	public void checkTemplate(String tpl, String compatibleDef, SqlSession sql, String purpose) throws EngineException
	{
		if (tpl != null)
		{
			if (!msgTplDB.exists(tpl, sql))
				throw new WrongArgumentException("Form has an unknown message template '" + tpl + "'");
			if (!compatibleDef.equals(msgTplDB.get(tpl, sql).getConsumer()))
				throw new WrongArgumentException("Template '" + tpl + 
						"' is not suitable as the " + purpose + " template");
		}
	}
}
