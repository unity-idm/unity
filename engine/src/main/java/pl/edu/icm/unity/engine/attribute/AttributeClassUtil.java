/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.generic.AttributeClassDB;

/**
 * Allows for creating {@link AttributesClassHelper}es easily.
 * 
 * @author K. Benedyczak
 */
@Component
public class AttributeClassUtil
{
	private static final AttributeClassHelper EMPTY_AC_HELPER = new AttributeClassHelper();
	public static final String ATTRIBUTE_CLASSES_ATTRIBUTE = "sys:AttributeClasses"; 
	
	private AttributeDAO dbAttributes; 
	private AttributeClassDB acDB;
	private GroupDAO dbGroups;
	
	@Autowired
	public AttributeClassUtil(AttributeDAO dbAttributes, AttributeClassDB acDB,
			GroupDAO dbGroups)
	{
		this.dbAttributes = dbAttributes;
		this.acDB = acDB;
		this.dbGroups = dbGroups;
	}

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
	
	/**
	 * Creates and returns {@link AttributeClassHelper} initialized with the current state of attribute classes
	 * for the given entity in a given group. Useful for testing whether changed attributes will match ACs.
	 * @param entityId
	 * @param groupPath
	 * @return
	 * @throws EngineException
	 */
	public AttributeClassHelper getACHelper(long entityId, String groupPath) throws EngineException
	{
		Collection<AttributeExt> acAttrs = dbAttributes.getEntityAttributes(entityId, 
				ATTRIBUTE_CLASSES_ATTRIBUTE, groupPath);
		Group group = dbGroups.get(groupPath);
		AttributeExt ac = null;
		if (acAttrs.size() != 0)
			ac = acAttrs.iterator().next();
		return getACHelperGeneric(group.getAttributesClasses(), ac == null ? null : ac.getValues());
	}

	/**
	 * Creates and returns {@link AttributeClassHelper} initialized with the given set of attribute classes
	 * for the given entity in a given group. Entity's ACs are added to the given set of ACs.
	 * Useful for testing updated group's ACs.
	 * @param entityId
	 * @param groupPath
	 * @return
	 * @throws EngineException
	 */
	public AttributeClassHelper getACHelper(long entityId, String groupPath, Set<String> acs) 
			throws EngineException
	{
		Collection<AttributeExt> acAttrs = dbAttributes.getEntityAttributes(entityId, 
				ATTRIBUTE_CLASSES_ATTRIBUTE, groupPath);
		AttributeExt ac = null;
		if (acAttrs.size() != 0)
			ac = (AttributeExt) acAttrs.iterator().next();
		
		return getACHelperGeneric(acs, ac == null ? null : ac.getValues());
	}

	/**
	 * Creates and returns {@link AttributeClassHelper} initialized with the given set of attribute classes
	 * for the given entity in a given group. Group's ACs are added to the given set of ACs.
	 * Useful for testing updated entity's ACs.
	 * @param entityId
	 * @param groupPath
	 * @param acs
	 * @return
	 * @throws EngineException
	 */
	public AttributeClassHelper getACHelper(String groupPath, Collection<String> acs) throws EngineException
	{
		Group group = dbGroups.get(groupPath);
		return getACHelperGeneric(group.getAttributesClasses(), acs);
	}
	
	private AttributeClassHelper getACHelperGeneric(Collection<String> groupAcs, 
			Collection<String> entityAcs) throws EngineException
	{
		Set<String> allAcs = new HashSet<>();
		if (entityAcs != null)
			allAcs.addAll(entityAcs);
		if (groupAcs != null)
			allAcs.addAll(groupAcs);
		if (allAcs.size() != 0)
		{
			Map<String, AttributesClass> allClasses = acDB.getAllAsMap();
			return new AttributeClassHelper(allClasses, allAcs);
		}
		return EMPTY_AC_HELPER;
	} 
}
