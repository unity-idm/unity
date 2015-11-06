/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.msgtemplates.MessageTemplateValidator;
import pl.edu.icm.unity.msgtemplates.MessageTemplateValidator.IllegalVariablesException;
import pl.edu.icm.unity.msgtemplates.MessageTemplateValidator.MandatoryVariablesException;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.registries.MessageTemplateConsumersRegistry;

/**
 * Implementation of {@link MessageTemplateManagement}
 * 
 * @author P. Piernik
 */
@Component
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
		SqlSession sql = SqlSessionTL.get();
		mtDB.insert(toAdd.getName(), toAdd, sql);
	}

	@Transactional
	@Override
	public void removeTemplate(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		mtDB.remove(name, sql);
	}

	@Transactional
	@Override
	public void updateTemplate(MessageTemplate updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateMessageTemplate(updated);
		SqlSession sql = SqlSessionTL.get();
		mtDB.update(updated.getName(), updated, sql);
	}

	@Transactional(noTransaction=true)
	@Override
	public Map<String, MessageTemplate> listTemplates() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		return mtDB.getAllAsMap(sql);
	}

	@Transactional(noTransaction=true)
	@Override
	public MessageTemplate getTemplate(String name) throws EngineException
	{	
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		return mtDB.get(name, sql);
	}

	@Transactional(noTransaction=true)
	@Override
	public Map<String, MessageTemplate> getCompatibleTemplates(String templateConsumer) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		Map<String, MessageTemplate> all = mtDB.getAllAsMap(sql);
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
