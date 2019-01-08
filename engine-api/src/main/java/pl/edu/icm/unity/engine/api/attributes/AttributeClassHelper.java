/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;

/**
 * Helper for handling {@link AttributesClass}es of an entity. A single virtual 'class' is created
 * from the classes provided as arguments. With it checking of attributes is fast.
 * <p>
 * Additionally static utility methods are provided allowing to clean up the duplicated entries in ACs.
 * 
 * @author K. Benedyczak
 */
public class AttributeClassHelper
{
	public static final int MAX_CLASSES_PER_ENTITY = 20;
	
	private Map<String, AttributesClass> allClasses;
	private AttributesClass effectiveClass;

	public AttributeClassHelper()
	{
		effectiveClass = new AttributesClass("", "", new HashSet<String>(0), 
				new HashSet<String>(0), true, new HashSet<String>(0));
	}
	
	public AttributeClassHelper(Map<String, AttributesClass> knownClasses, Collection<String> assignedClasses) 
	{
		if (assignedClasses.size() > MAX_CLASSES_PER_ENTITY)
			throw new RuntimeEngineException("Maximum number of attribute classes assigned" +
					" to entity is " + MAX_CLASSES_PER_ENTITY);
		allClasses = knownClasses;
		effectiveClass = new AttributesClass();
		for (String assignedClass: assignedClasses)
		{
			AttributesClass existing = allClasses.get(assignedClass);
			if (existing == null)
				throw new RuntimeEngineException("The attribute class " + assignedClass + 
						" is not defined");
			addAllFromClass(existing, effectiveClass);
		}
		//special case - no ACs = no restrictions
		if (assignedClasses.isEmpty())
			effectiveClass.setAllowArbitrary(true);
	}

	/**
	 * All parents which are also parents of other existing parents are removed.
	 * All allowed/mandatory attributes which are allowed/required by parent classes are removed.
	 * @param knownClasses
	 * @param toCleanup
	 * @return
	 * @throws IllegalTypeException 
	 */
	public static void cleanupClass(AttributesClass toCleanup, Map<String, AttributesClass> knownClasses) 
			throws IllegalTypeException
	{
		//important: no classes is a special case for ACHelper, but no parent classes is not, so simply skip.
		if (toCleanup.getParentClasses().isEmpty())
			return;
		
		cleanupParents(toCleanup, knownClasses);
			
		AttributeClassHelper helper = new AttributeClassHelper(knownClasses, toCleanup.getParentClasses());
		if (helper.isEffectiveAllowArbitrary())
		{
			toCleanup.getAllowed().clear();
			toCleanup.setAllowArbitrary(true);
		} else
		{
			removeIncluded(toCleanup.getAllowed(), helper, true);
		}
		removeIncluded(toCleanup.getMandatory(), helper, false);
	}
	
	/**
	 * @param original
	 * @return true only if the this effective class is more restrictive then the given argument,
	 * i.e. it has more mandatory attributes or less allowed.
	 */
	public boolean isRestricting(AttributeClassHelper original)
	{
		
		if (!original.getEffectiveMandatory().containsAll(getEffectiveMandatory()))
			return true;
		if (isEffectiveAllowArbitrary())
			return false;
		if (!getEffectiveAllowed().containsAll(original.getEffectiveAllowed()))
			return true;
		return false;
	}
	
	private static void removeIncluded(Set<String> set, AttributeClassHelper helper, boolean mode)
	{
		Iterator<String> aIt = set.iterator();
		while (aIt.hasNext())
		{
			String a = aIt.next();
			if (mode ? helper.isAllowed(a) : helper.isMandatory(a))
				aIt.remove();
		}
	}
	
	private static void cleanupParents(AttributesClass toCleanup, Map<String, AttributesClass> knownClasses)
	{
		Set<String> parentsToClean = toCleanup.getParentClasses();
		Set<String> cleanedParents = new HashSet<>(parentsToClean);
		for (String p: parentsToClean)
		{
			Set<String> includedParents = new HashSet<>();
			getAllParentsRec(p, knownClasses, includedParents);
			cleanedParents.removeAll(includedParents);
		}
		toCleanup.setParentClasses(cleanedParents);
	}
	
	private static void getAllParentsRec(String from, Map<String, AttributesClass> knownClasses, Set<String> ret)
	{
		Set<String> parents = knownClasses.get(from).getParentClasses();
		ret.addAll(parents);
		for (String p: parents)
			getAllParentsRec(p, knownClasses, ret);
	}
	
	
	/**
	 * @param clazz class to process
	 * @param allowed current allowed attributes. null if all are allowed.
	 * @param mandatory current mandatory attributes
	 * @return allowed
	 */
	private void addAllFromClass(AttributesClass clazz, AttributesClass effective)
	{
		effective.getMandatory().addAll(clazz.getMandatory());
		effective.getAllowed().addAll(clazz.getAllowed());
		if (clazz.isAllowArbitrary())
			effective.setAllowArbitrary(true);
		Set<String> parents = clazz.getParentClasses();
		for (String parent: parents)
			addAllFromClass(allClasses.get(parent), effective);
	}
	
	/**
	 * Verifies if the given attribute set is consistent with the effective AC
	 * @param attributes
	 * @param allTypes if not null, then it is used skip checking of attribute allowance in case of system attributes
	 * (i.e. those with instances immutable flag).
	 * @throws SchemaConsistencyException 
	 */
	public void checkAttribtues(Collection<String> attributes, Map<String, AttributeType> allTypes) 
			throws SchemaConsistencyException 
	{
		Set<String> mandatory = new HashSet<>(effectiveClass.getMandatory());
		if (allTypes == null)
			allTypes = Collections.emptyMap();
		
		for (String name: attributes)
		{
			AttributeType at = allTypes.get(name);
			boolean system = at == null ? false : at.isInstanceImmutable();
			if (!system && !effectiveClass.isAllowedDirectly(name))
				throw new SchemaConsistencyException("The assigned attribute " + name + 
						" is not allowed by the attribute classes being assinged");
			mandatory.remove(name);
		}
		if (mandatory.size() > 0)
			throw new SchemaConsistencyException("The following attributes" +
					" which are mandatory with respect to the attribute classes being assinged are not assigned: " 
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
	
	public Set<String> getEffectiveAllowed()
	{
		return effectiveClass.getAllowed();
	}
	
	public Set<String> getEffectiveMandatory()
	{
		return effectiveClass.getMandatory();
	}
	
	public boolean isEffectiveAllowArbitrary()
	{
		return effectiveClass.isAllowArbitrary();
	}
}
