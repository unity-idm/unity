/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.db.json.AttributeClassSerializer;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;

/**
 * Helper for handling {@link AttributesClass}es of an entity. A single virtual 'class' is created
 * from the classes provided as arguments. With it checking of attributes is fast.
 * 
 * @author K. Benedyczak
 */
public class AttributeClassHelper
{
	private static final AttributeClassHelper EMPTY_AC_HELPER = new AttributeClassHelper();
	public static final String ATTRIBUTE_CLASS_OBJECT_TYPE = "attributeClass";
	public static final String ATTRIBUTE_CLASSES_ATTRIBUTE = "sys:AttributeClasses"; 
	
	public static final int MAX_CLASSES_PER_ENTITY = 20;
	
	private Map<String, AttributesClass> allClasses;
	private AttributesClass effectiveClass;

	public AttributeClassHelper()
	{
		effectiveClass = new AttributesClass("", "", new HashSet<String>(0), 
				new HashSet<String>(0), true, null);
	}
	
	public AttributeClassHelper(Map<String, AttributesClass> knownClasses, Collection<String> assignedClasses) 
			throws IllegalTypeException
	{
		if (assignedClasses.size() > MAX_CLASSES_PER_ENTITY)
			throw new IllegalTypeException("Maximum number of attribute classes assigned" +
					" to entity is " + MAX_CLASSES_PER_ENTITY);
		allClasses = knownClasses;
		Set<String> allowed = new HashSet<>(50); 
		Set<String> mandatory = new HashSet<>(10); 
		boolean allAllowed = false;
		for (String assignedClass: assignedClasses)
		{
			AttributesClass existing = allClasses.get(assignedClass);
			if (existing == null)
				throw new IllegalTypeException("The attribute class " + assignedClass + 
						" is not defined");
			allAllowed |= addAllFromClass(existing, allowed, mandatory) == null;
		}
		if (assignedClasses.isEmpty())
			allAllowed = true;
		effectiveClass = new AttributesClass("", "", allowed, mandatory, allAllowed, null);
	}

	public static void validateAttributeClasses(Collection<String> toCheck, DBGeneric dbGeneric, SqlSession sql) 
			throws IllegalTypeException
	{
		if (toCheck == null)
			return;
		List<GenericObjectBean> raw = dbGeneric.getObjectsOfType(ATTRIBUTE_CLASS_OBJECT_TYPE, sql);
		Set<String> available = new HashSet<>(raw.size());
		for (GenericObjectBean b: raw)
			available.add(b.getName());
		for (String check: toCheck)
			if (!available.contains(check))
				throw new IllegalTypeException("Attributes class " + check + " is not available");
	}
	
	public static Map<String, AttributesClass> resolveAttributeClasses(DBGeneric dbGeneric, SqlSession sql)
	{
		List<GenericObjectBean> raw = dbGeneric.getObjectsOfType(ATTRIBUTE_CLASS_OBJECT_TYPE, sql);
		return resolveAttributeClasses(raw);
	}
	
	public static Map<String, AttributesClass> resolveAttributeClasses(List<GenericObjectBean> raw)
	{
		Map<String, AttributesClass> allClasses = new HashMap<>(raw.size());
		for (GenericObjectBean rawA: raw)
			allClasses.put(rawA.getName(), AttributeClassSerializer.deserialize(rawA.getContents()));
		return allClasses;
	}
	
	/**
	 * Creates and returns {@link AttributeClassHelper} initialized with the current state of attribute classes
	 * for the given entity in a given group. Useful for testing whether changed attributes will match ACs.
	 * @param entityId
	 * @param groupPath
	 * @param dbAttributes
	 * @param dbGeneric
	 * @param dbGroups
	 * @param sql
	 * @return
	 * @throws EngineException
	 */
	@SuppressWarnings("unchecked")
	public static AttributeClassHelper getACHelper(long entityId, String groupPath, DBAttributes dbAttributes, 
			DBGeneric dbGeneric, DBGroups dbGroups, SqlSession sql) throws EngineException
	{
		Collection<AttributeExt<?>> acAttrs = dbAttributes.getAllAttributes(entityId, 
				groupPath, false, ATTRIBUTE_CLASSES_ATTRIBUTE, sql);
		Group group = dbGroups.getContents(groupPath, GroupContents.METADATA, sql).getGroup();
		AttributeExt<String> ac = null;
		if (acAttrs.size() != 0)
			ac = (AttributeExt<String>) acAttrs.iterator().next();
		return getACHelperGeneric(group.getAttributesClasses(), ac == null ? null : ac.getValues(), dbGeneric, sql);
	}

	/**
	 * Creates and returns {@link AttributeClassHelper} initialized with the given set of attribute classes
	 * for the given entity in a given group. Entity's ACs are added to the given set of ACs.
	 * Useful for testing updated group's ACs.
	 * @param entityId
	 * @param groupPath
	 * @param dbAttributes
	 * @param dbGeneric
	 * @param dbGroups
	 * @param sql
	 * @return
	 * @throws EngineException
	 */
	@SuppressWarnings("unchecked")
	public static AttributeClassHelper getACHelper(long entityId, String groupPath, DBAttributes dbAttributes, 
			DBGeneric dbGeneric, Set<String> acs, SqlSession sql) throws EngineException
	{
		Collection<AttributeExt<?>> acAttrs = dbAttributes.getAllAttributes(entityId, 
				groupPath, false, ATTRIBUTE_CLASSES_ATTRIBUTE, sql);
		AttributeExt<String> ac = null;
		if (acAttrs.size() != 0)
			ac = (AttributeExt<String>) acAttrs.iterator().next();
		
		return getACHelperGeneric(acs, ac == null ? null : ac.getValues(), dbGeneric, sql);
	}

	/**
	 * Creates and returns {@link AttributeClassHelper} initialized with the given set of attribute classes
	 * for the given entity in a given group. Group's ACs are added to the given set of ACs.
	 * Useful for testing updated entity's ACs.
	 * @param entityId
	 * @param groupPath
	 * @param acs
	 * @param dbGeneric
	 * @param dbGroups
	 * @param sql
	 * @return
	 * @throws EngineException
	 */
	public static AttributeClassHelper getACHelper(String groupPath, Collection<String> acs,  
			DBGeneric dbGeneric, DBGroups dbGroups, SqlSession sql) throws EngineException
	{
		Group group = dbGroups.getContents(groupPath, GroupContents.METADATA, sql).getGroup();
		return getACHelperGeneric(group.getAttributesClasses(), acs, dbGeneric, sql);
	}
	
	private static AttributeClassHelper getACHelperGeneric(Collection<String> groupAcs, Collection<String> entityAcs,   
			DBGeneric dbGeneric, SqlSession sql) throws EngineException
	{
		Set<String> allAcs = new HashSet<>();
		if (entityAcs != null)
			allAcs.addAll(entityAcs);
		if (groupAcs != null)
			allAcs.addAll(groupAcs);
		if (allAcs.size() != 0)
		{
			Map<String, AttributesClass> allClasses = resolveAttributeClasses(dbGeneric, sql);
			return new AttributeClassHelper(allClasses, allAcs);
		}
		return EMPTY_AC_HELPER;
	} 
	
	/**
	 * @param clazz class to process
	 * @param allowed current allowed attributes. null if all are allowed.
	 * @param mandatory current mandatory attributes
	 * @return allowed
	 */
	private Set<String> addAllFromClass(AttributesClass clazz, Set<String> allowed, Set<String> mandatory)
	{
		mandatory.addAll(clazz.getMandatory());
		if (allowed != null)
			allowed.addAll(clazz.getAllowed());
		String parent = clazz.getParentClassName();
		if (parent != null)
			addAllFromClass(allClasses.get(parent), allowed, mandatory);
		if (clazz.isAllowArbitrary())
			allowed = null;
		return allowed;
	}
	
	/**
	 * Verifies if the given attribute set is consistent with the effective AC
	 * @param attributes
	 * @param allTypes if not null, then it is used skip checking of attribute allowance in case of system attributes
	 * (i.e. those with instances immutable flag).
	 * @throws SchemaConsistencyException 
	 */
	public void checkAttribtues(Collection<String> attributes, List<AttributeType> allTypes) 
			throws SchemaConsistencyException 
	{
		Set<String> mandatory = new HashSet<>(effectiveClass.getMandatory());
		Map<String, AttributeType> typesMap;
		
		if (allTypes != null)
		{
			typesMap = new HashMap<>(allTypes.size());
			for (AttributeType at: allTypes)
				typesMap.put(at.getName(), at);
		} else
			typesMap = Collections.emptyMap();
		
		for (String name: attributes)
		{
			AttributeType at = typesMap.get(name);
			boolean system = at == null ? false : at.isInstanceImmutable();
			if (!system && !effectiveClass.isAllowedDirectly(name))
				throw new SchemaConsistencyException("The entity has " + name + " attribute," +
						" which is not allowed by the attribute classes being assinged");
			mandatory.remove(name);
		}
		if (mandatory.size() > 0)
			throw new SchemaConsistencyException("The entity does not have the following attributes" +
					" which are mandatory with respect to the attribute classes being assinged: " 
					+ mandatory.toString());
	}
	

	public boolean isAllowed(String attribute)
	{
		return effectiveClass.isAllowedDirectly(attribute);
	}

	public boolean isMandatory(String attribute)
	{
		return effectiveClass.isMandatoryDirectly(attribute);
	}
}
