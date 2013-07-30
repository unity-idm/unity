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
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;

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
	
	public static AttributeClassHelper getACHelper(long entityId, String groupPath, DBAttributes dbAttributes, 
			DBGeneric dbGeneric, SqlSession sql) throws EngineException
	{
		Collection<AttributeExt<?>> acAttrs = dbAttributes.getAllAttributes(entityId, 
				groupPath, false, ATTRIBUTE_CLASSES_ATTRIBUTE, sql);
		if (acAttrs.size() != 0)
		{
			@SuppressWarnings("unchecked")
			AttributeExt<String> ac = (AttributeExt<String>) acAttrs.iterator().next();
			Map<String, AttributesClass> allClasses = resolveAttributeClasses(dbGeneric, sql);
			return new AttributeClassHelper(allClasses, ac.getValues());
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
	
	public void checkAttribtues(Collection<AttributeExt<?>> attributes, List<AttributeType> allTypes) 
			throws IllegalAttributeValueException
	{
		Set<String> mandatory = new HashSet<>(effectiveClass.getMandatory());
		Map<String, AttributeType> typesMap = new HashMap<>(allTypes.size());
		for (AttributeType at: allTypes)
			typesMap.put(at.getName(), at);
			
		for (AttributeExt<?> attribute: attributes)
		{
			String name = attribute.getName();
			AttributeType at = typesMap.get(name);
			if (!at.isInstanceImmutable() && !effectiveClass.isAllowedDirectly(name))
				throw new IllegalAttributeValueException("The entity has " + name + " attribute," +
						" which is not allowed by the attribute classes being assinged");
			mandatory.remove(name);
		}
		if (mandatory.size() > 0)
			throw new IllegalAttributeValueException("The entity does not have the following attributes" +
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
