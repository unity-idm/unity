/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.db.AttributeClassHelper;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.json.AttributeClassSerializer;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.attributes.AttributeValueSyntaxFactory;
import pl.edu.icm.unity.server.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Implements attributes operations.
 * @author K. Benedyczak
 */
@Component
public class AttributesManagementImpl implements AttributesManagement
{
	private AttributeSyntaxFactoriesRegistry attrValueTypesReg;
	private DBSessionManager db;
	private DBGeneric dbGeneric;
	private DBAttributes dbAttributes;
	private DBIdentities dbIdentities;
	private DBGroups dbGroups;
	private IdentitiesResolver idResolver;
	private AuthorizationManager authz;
	
	@Autowired
	public AttributesManagementImpl(AttributeSyntaxFactoriesRegistry attrValueTypesReg,
			DBSessionManager db, DBGeneric dbGeneric, DBAttributes dbAttributes,
			DBIdentities dbIdentities, DBGroups dbGroups,
			IdentitiesResolver idResolver, AuthorizationManager authz)
	{
		this.attrValueTypesReg = attrValueTypesReg;
		this.db = db;
		this.dbGeneric = dbGeneric;
		this.dbAttributes = dbAttributes;
		this.dbIdentities = dbIdentities;
		this.dbGroups = dbGroups;
		this.idResolver = idResolver;
		this.authz = authz;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedAttributeValueTypes() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		Collection<AttributeValueSyntaxFactory<?>> all = attrValueTypesReg.getAll();
		String[] ret = new String[all.size()];
		Iterator<AttributeValueSyntaxFactory<?>> it = all.iterator();
		for (int i=0; it.hasNext(); i++)
			ret[i] = it.next().getId();
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addAttributeType(AttributeType toAdd) throws EngineException
	{
		toAdd.validateInitialization();
		if (toAdd.getFlags() != 0)
			throw new IllegalAttributeTypeException("Custom attribute types must not have any flags set");
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			dbAttributes.addAttributeType(toAdd, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateAttributeType(AttributeType at) throws EngineException
	{
		at.validateInitialization();
		if (at.getFlags() != 0)
			throw new IllegalAttributeTypeException("Custom attribute types must not have any flags set");
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			AttributeType atExisting = dbAttributes.getAttributeType(at.getName(), sql);
			if ((atExisting.getFlags() & AttributeType.TYPE_IMMUTABLE_FLAG) != 0)
				throw new IllegalAttributeTypeException("The attribute type with name " + at.getName() + 
						" can not be manually updated");
			
			dbAttributes.updateAttributeType(at, sql);
			if (!at.getValueType().getValueSyntaxId().equals(atExisting.getValueType().getValueSyntaxId()))
				clearAttributeExtractionFromIdentities(at.getName(), sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAttributeType(String id, boolean deleteInstances) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			AttributeType at = dbAttributes.getAttributeType(id, sql);
			if ((at.getFlags() & (AttributeType.TYPE_IMMUTABLE_FLAG | 
					AttributeType.INSTANCES_IMMUTABLE_FLAG)) != 0)
				throw new IllegalAttributeTypeException("The attribute type with name " + id + 
						" can not be manually removed");

			dbAttributes.removeAttributeType(id, deleteInstances, sql);
			clearAttributeExtractionFromIdentities(id, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	private void clearAttributeExtractionFromIdentities(String id, SqlSession sql)
	{
		List<IdentityType> identityTypes = dbIdentities.getIdentityTypes(sql);
		for (IdentityType idType: identityTypes)
		{
			Map<String, String> extractedMap = idType.getExtractedAttributes();
			Iterator<Map.Entry<String, String>> entries = extractedMap.entrySet().iterator();
			boolean updateIdType = false;
			while (entries.hasNext())
			{
				Map.Entry<String, String> extracted = entries.next();
				if (extracted.getValue().equals(id))
				{
					entries.remove();
					updateIdType = true;
				}
			}
			if (updateIdType)
			{
				dbIdentities.updateIdentityType(sql, idType);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<AttributeType> getAttributeTypes() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		SqlSession sql = db.getSqlSession(false);
		try
		{
			return dbAttributes.getAttributeTypes(sql);
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addAttributeClass(AttributesClass clazz) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		String addedName = clazz.getName();
		String addedParentName = clazz.getParentClassName();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<GenericObjectBean> existingRaw = dbGeneric.getObjectsOfType(
					AttributeClassHelper.ATTRIBUTE_CLASS_OBJECT_TYPE, sql);
			boolean parentFound = clazz.getParentClassName() == null;
			for (GenericObjectBean existing: existingRaw)
			{
				String c = existing.getName();
				if (addedName.equals(c))
					throw new WrongArgumentException("The attribute class " + addedName + " already exists");
				if (addedParentName != null && addedParentName.equals(c))
					parentFound = true;
			}
			if (!parentFound)
				throw new WrongArgumentException("The attribute class parent " + addedParentName + 
						" does not exist");

			byte[] contents = AttributeClassSerializer.serialize(clazz).getBytes(Constants.UTF); 
			dbGeneric.addObject(clazz.getName(), AttributeClassHelper.ATTRIBUTE_CLASS_OBJECT_TYPE, 
					null, contents, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAttributeClass(String id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			Map<String, AttributesClass> allClasses = resolveAttributeClasses(sql);
			for (AttributesClass ac: allClasses.values())
			{
				if (id.equals(ac.getParentClassName()))
					throw new SchemaConsistencyException("Can not remove attribute class " + id + 
							" as it is a parent of the attribute class " + ac.getName());
			}
			if (!allClasses.containsKey(id))
				throw new WrongArgumentException("The attribute class " + id + " does not exist");
			
			Set<Long> entities = dbAttributes.getEntitiesWithStringAttribute(SystemAttributeTypes.ATTRIBUTE_CLASSES, id, sql);
			if (entities.size() > 0)
			{
				String info = String.valueOf(entities.iterator().next());
				if (entities.size() > 1)
					info += " and " + (entities.size()-1) + " more";
				throw new SchemaConsistencyException("The attribute class " + id + 
						" can not be removed as there are entities with this class set. " +
						"Ids of entities: " + info);
			}
			
			Set<String> groupsUsing = dbGroups.getGroupsUsingAc(id, sql);
			if (groupsUsing.size() > 0)
				throw new SchemaConsistencyException("The attribute class " + id + 
						" can not be removed as there are groups with have this class set: " +
						groupsUsing.toString());
			
			dbGeneric.removeObject(id, AttributeClassHelper.ATTRIBUTE_CLASS_OBJECT_TYPE, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void updateAttributeClass(AttributesClass updated) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public Collection<AttributesClass> getAttributeClasses() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			Map<String, AttributesClass> allClasses = resolveAttributeClasses(sql);
			sql.commit();
			return allClasses.values();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void setEntityAttributeClasses(EntityParam entity, String group, Collection<String> classes)
			throws EngineException
	{
		entity.validateInitialization();
		authz.checkAuthorization(group, AuthzCapability.attributeModify);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			AttributeClassHelper acHelper = AttributeClassHelper.getACHelper(group, classes, 
					dbGeneric, dbGroups, sql);
			
			long entityId = idResolver.getEntityId(entity, sql);
			Collection<String> allAttributes = dbAttributes.getEntityInGroupAttributeNames(entityId, 
					group, sql);
			List<AttributeType> allTypes = dbAttributes.getAttributeTypes(sql);
			acHelper.checkAttribtues(allAttributes, allTypes);

			StringAttribute classAttr = new StringAttribute(SystemAttributeTypes.ATTRIBUTE_CLASSES, 
					group, AttributeVisibility.local, new ArrayList<>(classes));
			dbAttributes.addAttribute(entityId, classAttr, true, sql);
			
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	

	@Override
	public Collection<AttributesClass> getEntityAttributeClasses(EntityParam entity,
			String group) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sql);
			authz.checkAuthorization(group, AuthzCapability.read);
			Collection<AttributeExt<?>> attrs = dbAttributes.getAllAttributes(entityId, group, false, 
					SystemAttributeTypes.ATTRIBUTE_CLASSES, sql);
			if (attrs.size() == 0)
				return Collections.emptySet();
			AttributeExt<?> attr = attrs.iterator().next();
			@SuppressWarnings("unchecked")
			List<String> classes = (List<String>) attr.getValues();
			
			Map<String, AttributesClass> allClasses = resolveAttributeClasses(sql);
			Set<AttributesClass> ret = new HashSet<>(classes.size());
			for (String clazz: classes)
			{
				AttributesClass ac = allClasses.get(clazz);
				if (ac != null)
					ret.add(ac);
			}
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	private Map<String, AttributesClass> resolveAttributeClasses(SqlSession sql)
	{
		return AttributeClassHelper.resolveAttributeClasses(dbGeneric, sql);
	}

	/**
	 * Verifies if the attribute is allowed wrt attribute classes defined for the entity in the respective group.
	 * @param entityId
	 * @param attribute
	 * @param sql
	 * @throws EngineException
	 */
	private void checkIfAllowed(long entityId, String groupPath, String attributeTypeId, SqlSession sql) 
			throws EngineException
	{
		AttributeClassHelper acHelper = AttributeClassHelper.getACHelper(entityId, groupPath, 
				dbAttributes, dbGeneric, dbGroups, sql);
		if (!acHelper.isAllowed(attributeTypeId))
			throw new SchemaConsistencyException("The attribute with name " + attributeTypeId + 
					" is not allowed by the entity's attribute classes in the group " + groupPath);
	}

	/**
	 * Verifies if the attribute is allowed wrt attribute classes defined for the entity in the respective group.
	 * @param entityId
	 * @param attribute
	 * @param sql
	 * @throws EngineException
	 */
	private void checkIfMandatory(long entityId, String groupPath, String attributeTypeId, SqlSession sql) 
			throws EngineException
	{
		AttributeClassHelper acHelper = AttributeClassHelper.getACHelper(entityId, groupPath, 
				dbAttributes, dbGeneric, dbGroups, sql);
		if (acHelper.isMandatory(attributeTypeId))
			throw new SchemaConsistencyException("The attribute with name " + attributeTypeId + 
					" is required by the entity's attribute classes in the group " + groupPath);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> void setAttribute(EntityParam entity, Attribute<T> attribute, boolean update)
			throws EngineException
	{
		attribute.validateInitialization();
		entity.validateInitialization();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			//Important - attributes can be also set as a result of addMember and addEntity.
			//  when changing this method, verify if those needs an update too.
			long entityId = idResolver.getEntityId(entity, sql);
			AttributeType at = dbAttributes.getAttributeType(attribute.getName(), sql);
			if (at.isInstanceImmutable())
				throw new SchemaConsistencyException("The attribute with name " + at.getName() + 
						" can not be manually modified");
			
			authz.checkAuthorization(at.isSelfModificable() && authz.isSelf(entityId), 
					attribute.getGroupPath(), AuthzCapability.attributeModify);

			checkIfAllowed(entityId, attribute.getGroupPath(), attribute.getName(), sql);
			
			dbAttributes.addAttribute(entityId, attribute, update, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAttribute(EntityParam entity, String groupPath, String attributeTypeId)
			throws EngineException
	{
		if (groupPath == null)
			throw new IllegalGroupValueException("Group must not be null");
		if (attributeTypeId == null)
			throw new IllegalAttributeValueException("Attribute name must not be null");
		entity.validateInitialization();

		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sql);
			AttributeType at = dbAttributes.getAttributeType(attributeTypeId, sql);
			if (at.isInstanceImmutable())
				throw new SchemaConsistencyException("The attribute with name " + at.getName() + 
						" can not be manually modified");
			authz.checkAuthorization(at.isSelfModificable() && authz.isSelf(entityId),
					groupPath, AuthzCapability.attributeModify);
			
			checkIfMandatory(entityId, groupPath, attributeTypeId, sql);
			
			dbAttributes.removeAttribute(entityId, groupPath, attributeTypeId, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<AttributeExt<?>> getAttributes(EntityParam entity, String groupPath,
			String attributeTypeId) throws EngineException
	{
		Collection<AttributeExt<?>> ret = getAllAttributesInternal(entity, true, groupPath, attributeTypeId, 
				AuthzCapability.read, false);
		filterLocal(ret);
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<AttributeExt<?>> getAllAttributes(EntityParam entity, boolean effective, String groupPath,
			String attributeTypeId, boolean allowDegrade) throws EngineException
	{
		try
		{
			return getAllAttributesInternal(entity, effective, groupPath, attributeTypeId, 
					AuthzCapability.attributeModify, true);
		} catch (AuthorizationException e)
		{
			if (allowDegrade)
			{
				Collection<AttributeExt<?>> ret = getAllAttributesInternal(entity, effective, 
						groupPath, attributeTypeId, AuthzCapability.read, false);
				filterLocal(ret);
				return ret;
			} else
				throw e;
		}
	}

	private void filterLocal(Collection<AttributeExt<?>> unfiltered)
	{
		Iterator<AttributeExt<?>> it = unfiltered.iterator();
		while (it.hasNext())
		{
			AttributeExt<?> attr = it.next();
			if (attr.getVisibility() == AttributeVisibility.local)
				it.remove();
		}
	}
	
	private Collection<AttributeExt<?>> getAllAttributesInternal(EntityParam entity, boolean effective, String groupPath,
			String attributeTypeName, AuthzCapability requiredCapability, boolean allowDisabled) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sql);
			if (!allowDisabled)
			{
				EntityState state = dbIdentities.getEntityStatus(entityId, sql);
				if (state == EntityState.disabled)
					throw new IllegalIdentityValueException("The entity is disabled");
			}
			authz.checkAuthorization(authz.isSelf(entityId), groupPath, requiredCapability);
			Collection<AttributeExt<?>> ret = dbAttributes.getAllAttributes(entityId, groupPath, effective,
					attributeTypeName, sql);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}	
	}
}
