/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.notifications.MessageTemplate;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;

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
	
	
	@Autowired
	public MessageTemplateManagementImpl(DBSessionManager db, AuthorizationManager authz,
			MessageTemplateDB mtDB)
	{
		this.db = db;
		this.authz = authz;
		this.mtDB = mtDB;
	}
	
	@Override
	public void addTemplate(MessageTemplate toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
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
		MessageTemplate template;
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			template = mtDB.get(name, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		return template;
	}

}
