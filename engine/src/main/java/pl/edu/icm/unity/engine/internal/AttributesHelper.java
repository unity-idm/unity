/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBShared;
import pl.edu.icm.unity.db.generic.ac.AttributeClassDB;
import pl.edu.icm.unity.db.generic.ac.AttributeClassUtil;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.attributes.AttributeClassHelper;
import pl.edu.icm.unity.server.attributes.AttributeMetadataProvider;
import pl.edu.icm.unity.server.registries.AttributeMetadataProvidersRegistry;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;

/**
 * Attributes and ACs related operations, intended for reuse between other classes.
 * No operation in this interface performs any authorization.
 * @author K. Benedyczak
 */
@Component
public class AttributesHelper
{
	private AttributeMetadataProvidersRegistry atMetaProvidersRegistry;
	private DBAttributes dbAttributes;
	private AttributeClassDB acDB;
	private DBGroups dbGroups;
	private DBShared dbShared;
	private DBIdentities dbIdentities;
	private IdentitiesResolver idResolver;
	
	@Autowired
	public AttributesHelper(AttributeMetadataProvidersRegistry atMetaProvidersRegistry,
			DBAttributes dbAttributes, AttributeClassDB acDB, DBGroups dbGroups,
			DBShared dbShared, DBIdentities dbIdentities,
			IdentitiesResolver idResolver)
	{
		this.atMetaProvidersRegistry = atMetaProvidersRegistry;
		this.dbAttributes = dbAttributes;
		this.acDB = acDB;
		this.dbGroups = dbGroups;
		this.dbShared = dbShared;
		this.dbIdentities = dbIdentities;
		this.idResolver = idResolver;
	}


	public AttributeType getAttributeTypeWithSingeltonMetadata(String metadataId, SqlSession sql)
			throws EngineException
	{
		AttributeMetadataProvider provider = atMetaProvidersRegistry.getByName(metadataId);
		if (!provider.isSingleton())
			throw new WrongArgumentException("Metadata for this call must be singleton.");
		Collection<AttributeType> existingAts = dbAttributes.getAttributeTypes(sql).values();
		AttributeType ret = null;
		for (AttributeType at: existingAts)
			if (at.getMetadata().containsKey(metadataId))
				ret = at;
		return ret;
	}
	
	public AttributeExt<?> getAttributeByMetadata(EntityParam entity, String group,
			String metadataId, SqlSession sql) throws EngineException
	{
		AttributeType at = getAttributeTypeWithSingeltonMetadata(metadataId, sql);
		if (at == null)
			return null;
		long entityId = idResolver.getEntityId(entity, sql);
		Collection<AttributeExt<?>> ret = getAllAttributesInternal(sql, entityId, false, 
				group, at.getName(), false);
		return ret.size() == 1 ? ret.iterator().next() : null; 
	}

	
	/**
	 * Sets ACs of a given entity. Pure business logic - no authZ and transaction management.
	 * @param entityId
	 * @param group
	 * @param classes
	 * @param sql
	 * @throws EngineException
	 */
	public void setAttributeClasses(long entityId, String group, Collection<String> classes, SqlSession sql) 
			throws EngineException
	{
		AttributeClassHelper acHelper = AttributeClassUtil.getACHelper(group, classes, 
				acDB, dbGroups, sql);
		
		Collection<String> allAttributes = dbAttributes.getEntityInGroupAttributeNames(entityId, 
				group, sql);
		Map<String, AttributeType> allTypes = dbAttributes.getAttributeTypes(sql);
		acHelper.checkAttribtues(allAttributes, allTypes);

		StringAttribute classAttr = new StringAttribute(SystemAttributeTypes.ATTRIBUTE_CLASSES, 
				group, AttributeVisibility.local, new ArrayList<>(classes));
		dbAttributes.addAttribute(entityId, classAttr, true, sql);
	}
	
	
	public Collection<AttributeExt<?>> getAllAttributesInternal(SqlSession sql, long entityId, 
			boolean effective, String groupPath, String attributeTypeName, 
			boolean allowDisabled) throws EngineException
	{
		if (!allowDisabled)
		{
			EntityState state = dbIdentities.getEntityStatus(entityId, sql);
			if (state == EntityState.disabled)
				throw new IllegalIdentityValueException("The entity is disabled");
		}
		if (groupPath != null)
		{
			Set<String> allGroups = dbShared.getAllGroups(entityId, sql);
			if (!allGroups.contains(groupPath))
				throw new IllegalGroupValueException("The entity is not a member of the group " + groupPath);
		}
		Collection<AttributeExt<?>> ret = dbAttributes.getAllAttributes(entityId, groupPath, effective,
				attributeTypeName, sql);
		return ret;
	}
	
	/**
	 * Adds an attribute. This method performs engine level checks: whether the attribute type is not immutable,
	 * and properly sets unverified state if attribute is added by ordinary user (not an admin).
	 * @param sql
	 * @param entityId
	 * @param update
	 * @param at
	 * @param honorInitialConfirmation if true then operation is run by privileged user, otherwise it is modification of self 
	 * possessed attribute and verification status must be set to unverified.
	 * @param attribute
	 * @throws EngineException
	 */
	public <T> void addAttribute(SqlSession sql, long entityId, boolean update,
			AttributeType at, boolean honorInitialConfirmation, Attribute<T> attribute) throws EngineException
	{
		if (at.isInstanceImmutable())
			throw new SchemaConsistencyException("The attribute with name " + at.getName() + 
					" can not be manually modified");
		
		if (attribute.getAttributeSyntax().isVerifiable() && !honorInitialConfirmation)
		{
			//we must force verification status to false, as only real admin can set attributes in 
			//the confirmed state
			for (Object v : attribute.getValues())
			{
				VerifiableElement val = (VerifiableElement) v;
				val.setConfirmationInfo(new ConfirmationInfo(0));
			}
		}
		
		dbAttributes.addAttribute(entityId, attribute, update, sql);
	}

	/**
	 * Checks if the given set of attributes fulfills rules of ACs of a specified group 
	 * @throws EngineException 
	 */
	public void checkGroupAttributeClassesConsistency(List<Attribute<?>> attributes, String path, SqlSession sql) 
			throws EngineException
	{
		AttributeClassHelper helper = AttributeClassUtil.getACHelper(path, 
				new ArrayList<String>(0), acDB, dbGroups, sql);
		Set<String> attributeNames = new HashSet<>(attributes.size());
		for (Attribute<?> a: attributes)
			attributeNames.add(a.getName());
		helper.checkAttribtues(attributeNames, null);
	}

	/**
	 * Same as {@link #addAttribute(SqlSession, long, boolean, AttributeType, boolean, Attribute)}
	 * but for a whole list of attributes. It is assumed that attributes are always created. 
	 * Attribute type is automatically resolved.
	 *   
	 * @param attributes
	 * @param entityId
	 * @param honorInitialConfirmation
	 * @param sqlMap
	 * @throws EngineException
	 */
	public void addAttributesList(List<Attribute<?>> attributes, long entityId, boolean honorInitialConfirmation, 
			SqlSession sqlMap) throws EngineException
	{
		for (Attribute<?> a: attributes)
		{
			AttributeType at = dbAttributes.getAttributeType(a.getName(), sqlMap);
			addAttribute(sqlMap, entityId, false, at, honorInitialConfirmation, a);
		}
	}
}
