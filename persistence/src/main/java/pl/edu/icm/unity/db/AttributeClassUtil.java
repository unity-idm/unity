/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.Collection;
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
import pl.edu.icm.unity.server.attributes.AttributeClassHelper;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;

/**
 * Allows for creating {@link AttributesClassHelper}es easily.
 * 
 * @author K. Benedyczak
 */
public abstract class AttributeClassUtil
{
	private static final AttributeClassHelper EMPTY_AC_HELPER = new AttributeClassHelper();
	public static final String ATTRIBUTE_CLASS_OBJECT_TYPE = "attributeClass";
	public static final String ATTRIBUTE_CLASSES_ATTRIBUTE = "sys:AttributeClasses"; 
	
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
}
