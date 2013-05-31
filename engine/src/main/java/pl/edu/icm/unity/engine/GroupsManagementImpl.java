/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatementCondition;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;

/**
 * Implementation of groups management. Responsible for top level transaction handling,
 * proper error logging and authorization.
 * 
 * @author K. Benedyczak
 */
public class GroupsManagementImpl implements GroupsManagement
{
	private DBSessionManager db;
	private DBGroups dbGroups;
	private AuthorizationManager authz;
	private IdentitiesResolver idResolver;
	
	@Autowired
	public GroupsManagementImpl(DBSessionManager db, DBGroups dbGroups,
			AuthorizationManager authz, IdentitiesResolver idResolver)
	{
		this.db = db;
		this.dbGroups = dbGroups;
		this.authz = authz;
		this.idResolver = idResolver;
	}

	@Override
	public void addGroup(Group toAdd) throws EngineException
	{
		validateGroupStatements(toAdd);
		authz.checkAuthorization(toAdd.getParentPath(), AuthzCapability.groupModify);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			dbGroups.addGroup(toAdd, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void linkGroup(String targetPath, String sourcePath) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void unlinkGroup(String targetPath, String sourcePath) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void removeGroup(String path, boolean recursive) throws EngineException
	{
		authz.checkAuthorization(path, AuthzCapability.groupModify);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			dbGroups.removeGroup(path, recursive, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void addSelfManagedGroup(Group toAdd) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void addMemberFromParent(String path, EntityParam entity) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sql);
			authz.checkAuthorization(authz.isSelf(entityId), path, AuthzCapability.groupModify);
			dbGroups.addMemberFromParent(path, entity, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void removeMember(String path, EntityParam entity) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sql);
			authz.checkAuthorization(authz.isSelf(entityId), path, AuthzCapability.groupModify);
			dbGroups.removeMember(path, entity, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public GroupContents getContents(String path, int filter) throws EngineException
	{
		authz.checkAuthorization(path, AuthzCapability.read);
		SqlSession sql = db.getSqlSession(true);
		try 
		{
			GroupContents ret = dbGroups.getContents(path, filter, sql);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void updateGroup(String path, Group group) throws EngineException
	{
		validateGroupStatements(group);
		authz.checkAuthorization(path, AuthzCapability.groupModify);
		SqlSession sql = db.getSqlSession(true);
		try 
		{
			dbGroups.updateGroup(path, group, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	private void validateGroupStatements(Group group) throws IllegalAttributeValueException
	{
		AttributeStatement[] statements = group.getAttributeStatements();
		String path = group.toString();
		for (AttributeStatement statement: statements)
			validateGroupStatement(path, statement);
	}

	private void validateGroupStatement(String group, AttributeStatement statement) 
			throws IllegalAttributeValueException
	{
		String statementPath = statement.getAssignedAttribute().getGroupPath();
		if (!group.equals(statementPath))
			throw new IllegalAttributeValueException("The statement attribute must have " +
					"the same group scope as its group: " + group);
		AttributeStatementCondition condition = statement.getCondition();
		condition.validate(group);
	}
}
