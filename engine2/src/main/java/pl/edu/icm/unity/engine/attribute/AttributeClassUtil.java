/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.generic.AttributeClassDB;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Allows for creating {@link AttributesClassHelper}es easily.
 * 
 * @author K. Benedyczak
 */
public abstract class AttributeClassUtil
{
	private static final AttributeClassHelper EMPTY_AC_HELPER = new AttributeClassHelper();
	public static final String ATTRIBUTE_CLASSES_ATTRIBUTE = "sys:AttributeClasses"; 
	
	public static void validateAttributeClasses(Collection<String> toCheck, AttributeClassDB acDB) 
			throws EngineException
	{
		if (toCheck == null)
			return;
		Set<String> available = acDB.getAllNames();
		for (String check: toCheck)
			if (!available.contains(check))
				throw new IllegalTypeException("Attributes class " + check + " is not available");
	}
	
	public static Map<String, AttributesClass> resolveAttributeClasses(AttributeClassDB acDB) 
			throws EngineException
	{
		return acDB.getAllAsMap();
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
	public static AttributeClassHelper getACHelper(long entityId, String groupPath, AttributeDAO dbAttributes, 
			AttributeClassDB acDB, GroupDAO dbGroups) throws EngineException
	{
		Collection<AttributeExt> acAttrs = dbAttributes.getEntityAttributes(entityId, 
				ATTRIBUTE_CLASSES_ATTRIBUTE, groupPath);
		Group group = dbGroups.get(groupPath);
		AttributeExt ac = null;
		if (acAttrs.size() != 0)
			ac = acAttrs.iterator().next();
		return getACHelperGeneric(group.getAttributesClasses(), ac == null ? null : ac.getValues(), acDB);
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
	 * @return
	 * @throws EngineException
	 */
	public static AttributeClassHelper getACHelper(long entityId, String groupPath, AttributeDAO dbAttributes, 
			AttributeClassDB acDB, Set<String> acs) throws EngineException
	{
		Collection<AttributeExt> acAttrs = dbAttributes.getEntityAttributes(entityId, 
				ATTRIBUTE_CLASSES_ATTRIBUTE, groupPath);
		AttributeExt ac = null;
		if (acAttrs.size() != 0)
			ac = (AttributeExt) acAttrs.iterator().next();
		
		return getACHelperGeneric(acs, ac == null ? null : ac.getValues(), acDB);
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
	 * @return
	 * @throws EngineException
	 */
	public static AttributeClassHelper getACHelper(String groupPath, Collection<String> acs,  
			AttributeClassDB acDB, GroupDAO dbGroups) throws EngineException
	{
		Group group = dbGroups.get(groupPath);
		return getACHelperGeneric(group.getAttributesClasses(), acs, acDB);
	}
	
	private static AttributeClassHelper getACHelperGeneric(Collection<String> groupAcs, Collection<String> entityAcs,   
			AttributeClassDB acDB) throws EngineException
	{
		Set<String> allAcs = new HashSet<>();
		if (entityAcs != null)
			allAcs.addAll(entityAcs);
		if (groupAcs != null)
			allAcs.addAll(groupAcs);
		if (allAcs.size() != 0)
		{
			Map<String, AttributesClass> allClasses = resolveAttributeClasses(acDB);
			return new AttributeClassHelper(allClasses, allAcs);
		}
		return EMPTY_AC_HELPER;
	} 
}
