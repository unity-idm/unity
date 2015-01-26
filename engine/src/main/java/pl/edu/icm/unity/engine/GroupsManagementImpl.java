/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.DBShared;
import pl.edu.icm.unity.db.generic.ac.AttributeClassDB;
import pl.edu.icm.unity.db.generic.ac.AttributeClassUtil;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.internal.AttributesHelper;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.attributes.AttributeClassHelper;
import pl.edu.icm.unity.server.attributes.AttributeValueChecker;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
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
	private DBShared dbShared;
	private DBAttributes dbAttributes;
	private AttributeClassDB acDB;
	private AuthorizationManager authz;
	private AttributesHelper attributesHelper;
	private IdentitiesResolver idResolver;
	private ConfirmationManager confirmationManager;
	
	@Autowired
	public GroupsManagementImpl(DBSessionManager db, DBGroups dbGroups, DBShared dbShared,
			DBAttributes dbAttributes, AttributeClassDB acDB,
			AuthorizationManager authz, AttributesHelper attributesHelper,
			IdentitiesResolver idResolver, ConfirmationManager confirmationsManager)
	{
		this.db = db;
		this.dbGroups = dbGroups;
		this.dbShared = dbShared;
		this.dbAttributes = dbAttributes;
		this.acDB = acDB;
		this.authz = authz;
		this.attributesHelper = attributesHelper;
		this.idResolver = idResolver;
		this.confirmationManager = confirmationsManager;
	}

	@Override
	public void addGroup(Group toAdd) throws EngineException
	{
		authz.checkAuthorization(toAdd.getParentPath(), AuthzCapability.groupModify);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			validateGroupStatements(toAdd, sql);
			AttributeClassUtil.validateAttributeClasses(toAdd.getAttributesClasses(), acDB, sql);
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
		addMemberFromParent(path, entity, null);
	}
	

	@Override
	public void addMemberFromParent(String path, EntityParam entity,
			List<Attribute<?>> attributes) throws EngineException
	{
		entity.validateInitialization();
		if (attributes == null)
			attributes = Collections.emptyList();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sql);
			authz.checkAuthorization(authz.isSelf(entityId), path, AuthzCapability.groupModify);
			
			attributesHelper.checkGroupAttributeClassesConsistency(attributes, path, sql);
			
			dbGroups.addMemberFromParent(path, entity, sql);

			attributesHelper.addAttributesList(attributes, entityId, true, sql);
			sql.commit();
			//careful - must be after the transaction is committed
			confirmationManager.sendVerificationsQuiet(entity, attributes);
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
		try
		{
			authz.checkAuthorization(path, AuthzCapability.read);
		} catch (AuthorizationException e)
		{
			if ((GroupContents.GROUPS & filter) == 0)
				throw e;
			return getLimitedContents(path, filter);
		}
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

	/**
	 * Invoked whenever getContents fails due to insufficient authZ. In such case still some subgroups
	 * of the given group can be returned, only if the requester is their member.
	 * @param path
	 * @param filter
	 * @return
	 * @throws EngineException
	 */
	private GroupContents getLimitedContents(String path, int filter) throws EngineException
	{
		long entity = InvocationContext.getCurrent().getLoginSession().getEntityId();
		authz.checkAuthorization(true, AuthzCapability.read);
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			Set<String> allGroups = dbShared.getAllGroups(entity, sqlMap);
			sqlMap.commit();
			if (!allGroups.contains(path))
				throw new AuthorizationException("Access is denied. The operation "
						+ "getContents requires 'read' capability");
				
			//TODO - handle linked groups
			List<String> directSubgroups = new ArrayList<String>();
			
			for (String g: allGroups)
			{
				Group potential = new Group(g);
				String parent = potential.getParentPath();
				if (parent != null && parent.equals(path))
					directSubgroups.add(g);
			}
			
			GroupContents ret = new GroupContents();
			ret.setSubGroups(directSubgroups);
			
			if ((filter & GroupContents.LINKED_GROUPS) != 0)
			{
				ret.setLinkedGroups(new ArrayList<String>());
			}
			if ((filter & GroupContents.MEMBERS) != 0)
			{
				ret.setMembers(new ArrayList<Long>());
			}
			if ((filter & GroupContents.METADATA) != 0)
			{
				ret.setGroup(new Group(path));
			}
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}
	
	@Override
	public void updateGroup(String path, Group group) throws EngineException
	{
		authz.checkAuthorization(path, AuthzCapability.groupModify);
		SqlSession sql = db.getSqlSession(true);
		try 
		{
			validateGroupStatements(group, sql);
			AttributeClassUtil.validateAttributeClasses(group.getAttributesClasses(), acDB, sql);
			GroupContents gc = dbGroups.getContents(path, GroupContents.MEMBERS, sql);
			Map<String, AttributeType> allTypes = dbAttributes.getAttributeTypes(sql);
			for (Long entity: gc.getMembers())
			{
				AttributeClassHelper helper = AttributeClassUtil.getACHelper(entity, path, 
						dbAttributes, acDB, group.getAttributesClasses(), sql);
				Collection<String> attributes = dbAttributes.getEntityInGroupAttributeNames(
						entity, path, sql);
				helper.checkAttribtues(attributes, allTypes);
			}
			dbGroups.updateGroup(path, group, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	private void validateGroupStatements(Group group, SqlSession sql) throws IllegalAttributeValueException, 
		IllegalAttributeTypeException, IllegalTypeException
	{
		AttributeStatement[] statements = group.getAttributeStatements();
		String path = group.toString();
		for (AttributeStatement statement: statements)
			validateGroupStatement(path, statement, sql);
	}

	private void validateGroupStatement(String group, AttributeStatement statement, SqlSession sql) 
			throws IllegalAttributeValueException, IllegalAttributeTypeException, IllegalTypeException
	{
		statement.validate(group);
		Attribute<?> attribute = statement.getAssignedAttribute();
		String attributeName = statement.getAssignedAttributeName();
		AttributeType at = dbAttributes.getAttributeType(attributeName, sql);
		if (at.isInstanceImmutable())
			throw new IllegalAttributeTypeException("Can not assign attribute " + at.getName() +
					" in attribute statement as the attribute type is an internal, system attribute.");
		if (statement.getConflictResolution() != ConflictResolution.merge && attribute != null)
		{
			AttributeValueChecker.validate(attribute, at);
		}
		Attribute<?> conditionAttr = statement.getConditionAttribute();
		if (conditionAttr != null)
		{
			dbAttributes.getAttributeType(conditionAttr.getName(), sql);
		}
	}
}
