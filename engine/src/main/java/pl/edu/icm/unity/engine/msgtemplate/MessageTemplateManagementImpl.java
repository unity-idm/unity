/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.msgtemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msgtemplates.GenericMessageTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateValidator.IllegalVariablesException;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateValidator.MandatoryVariablesException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.MessageTemplate;

/**
 * Implementation of {@link MessageTemplateManagement}
 * 
 * @author P. Piernik
 */
@Component
@Primary
@InvocationEventProducer
public class MessageTemplateManagementImpl implements MessageTemplateManagement
{
	private AuthorizationManager authz;
	private MessageTemplateDB mtDB;
	private MessageTemplateConsumersRegistry registry;
	
	
	@Autowired
	public MessageTemplateManagementImpl(AuthorizationManager authz,
			MessageTemplateDB mtDB, MessageTemplateConsumersRegistry registry)
	{
		this.authz = authz;
		this.mtDB = mtDB;
		this.registry = registry;
	}
	
	@Transactional
	@Override
	public void addTemplate(MessageTemplate toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateMessageTemplate(toAdd);
		mtDB.create(toAdd);
	}

	@Transactional
	@Override
	public void removeTemplate(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		mtDB.delete(name);
	}

	@Transactional
	@Override
	public void updateTemplate(MessageTemplate updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateMessageTemplate(updated);
		mtDB.update(updated);
	}

	@Transactional
	@Override
	public Map<String, MessageTemplate> listTemplates() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return mtDB.getAllAsMap();
	}

	@Transactional
	@Override
	public MessageTemplate getTemplate(String name) throws EngineException
	{	
		authz.checkAuthorization(AuthzCapability.maintenance);
		return mtDB.get(name);
	}

	@Transactional
	@Override
	public MessageTemplate getPreprocessedTemplate(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		Map<String, MessageTemplate> allAsMap = mtDB.getAllAsMap();
		MessageTemplate requested = allAsMap.get(name);
		if (requested == null)
			throw new IllegalArgumentException("There is no message template " + name);
		Map<String, MessageTemplate> genericTemplates = allAsMap.entrySet().stream()
			.filter(e -> e.getValue().getConsumer().equals(GenericMessageTemplateDef.NAME))
			.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
		return requested.preprocessMessage(genericTemplates);
	}
	
	@Transactional
	@Override
	public Map<String, MessageTemplate> getCompatibleTemplates(String templateConsumer) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		Map<String, MessageTemplate> all = mtDB.getAllAsMap();
		Map<String, MessageTemplate> ret = new HashMap<String, MessageTemplate>(all);
		for (MessageTemplate m : all.values())
		{
			if (!m.getConsumer().equals(templateConsumer))
			{
				ret.remove(m.getName());
			}
		}
		return ret;
	}
	
	private void validateMessageTemplate(MessageTemplate toValidate)
			throws EngineException
	{
		MessageTemplateDefinition con = registry.getByName(toValidate.getConsumer());
		try
		{
			MessageTemplateValidator.validateMessage(con, toValidate.getMessage());
		} catch (IllegalVariablesException e)
		{
			throw new WrongArgumentException("The following variables are unknown: " + e.getUnknown());
		} catch (MandatoryVariablesException e)
		{
			throw new WrongArgumentException("The following variables must be used: " + e.getMandatory());
		}
	}
}
