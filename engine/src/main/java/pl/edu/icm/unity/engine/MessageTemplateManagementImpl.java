/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplate.Message;
import pl.edu.icm.unity.msgtemplates.MessageTemplateConsumer;
import pl.edu.icm.unity.msgtemplates.MessageTemplateValidator;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.registries.MessageTemplateConsumersRegistry;

/**
 * Implementation of {@link MessageTemplateManagement}
 * 
 * @author P. Piernik
 */
public class MessageTemplateManagementImpl implements MessageTemplateManagement
{
	private DBSessionManager db;
	private AuthorizationManager authz;
	private MessageTemplateDB mtDB;
	private MessageTemplateConsumersRegistry registry;
	
	
	@Autowired
	public MessageTemplateManagementImpl(DBSessionManager db, AuthorizationManager authz,
			MessageTemplateDB mtDB, MessageTemplateConsumersRegistry registry)
	{
		this.db = db;
		this.authz = authz;
		this.mtDB = mtDB;
		this.registry = registry;
	}
	
	@Override
	public void addTemplate(MessageTemplate toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateMessageTemplate(toAdd);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			mtDB.insert(toAdd.getName(), toAdd, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}

	}

	@Override
	public void removeTemplate(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			mtDB.remove(name, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}

	}

	@Override
	public void updateTemplate(MessageTemplate updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateMessageTemplate(updated);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			mtDB.update(updated.getName(), updated, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}

	}

	@Override
	public Map<String, MessageTemplate> listTemplates() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(false);
		try
		{
			Map<String, MessageTemplate> ret = mtDB.getAllAsMap(sql);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public MessageTemplate getTemplate(String name) throws EngineException
	{	
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			MessageTemplate template = mtDB.get(name, sql);
			sql.commit();
			return template;
		} finally
		{
			db.releaseSqlSession(sql);
		}
		
	}

	@Override
	public Map<String, MessageTemplate> getCompatibleTemplates(String templateConsumer) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(false);
		try
		{
			Map<String, MessageTemplate> all = mtDB.getAllAsMap(sql);
			Map<String, MessageTemplate> ret = new HashMap<String, MessageTemplate>(all);
			for (MessageTemplate m : all.values())
			{
				if (!m.getConsumer().equals(templateConsumer))
				{
					ret.remove(m.getName());
				}
			}
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
		
	}
	
	private void validateMessageTemplate(MessageTemplate toValidate)
			throws EngineException
	{
		MessageTemplateConsumer con = registry.getByName(toValidate.getConsumer());
		if (con == null)
		{
			throw new WrongArgumentException("The consumer is unknown");
		}
		MessageTemplateValidator validator = new MessageTemplateValidator(con);
		for (Message t : toValidate.getAllMessages().values())
		{
			if (!(validator.validateText(t.getSubject()) && validator.validateText(t
					.getBody())))
			{
				throw new WrongArgumentException(
						"The vars used in subject or body are not compatible with consumer");
			}
		}
	}
}
