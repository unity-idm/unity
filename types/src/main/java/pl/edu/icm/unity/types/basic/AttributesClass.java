/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.HashSet;
import java.util.Set;

import pl.edu.icm.unity.types.DescribedObjectImpl;

/**
 * Represents attribute class, i.e. a structure with two sets of attribute types. The mandatory set
 * defines which attributes are mandatory for an entity having the class 
 * and allowed - which are allowed. It is possible to set that all attributes are allowed.
 * All mandatory attributes are always allowed.
 * <p>
 * Attribute class can have a parents. Then all mandatory and allowed attribute types of the parent are
 * added to those defined locally.
 * <p>
 * Attribute class inheritance can be multilevel.
 * <p>
 * Attribute class is always assigned in a scope of a particular group, but the group is not defined in 
 * the class itself, i.e. the class can be used in many groups. 
 * <p>
 * If an entity has multiple attribute classes assigned in some group, then all sets are summed up. If any of the
 * classes allows for arbitrary attributes, then effectively all attributes are allowed.
 * <p>
 * All mandatory attributes are always allowed.
 * @author K. Benedyczak
 */
public class AttributesClass extends DescribedObjectImpl
{
	private Set<String> allowed;
	private Set<String> mandatory;
	private boolean allowArbitrary;
	private Set<String> parentClasses;
	
	
	public AttributesClass(String name, String description, Set<String> allowed, Set<String> mandatory, 
			boolean allowArbitrary, Set<String> parentClasses)
	{
		super(name, description);
		setParentClassName(parentClasses);
		setAllowed(allowed);
		setMandatory(mandatory);
		this.allowed.addAll(mandatory);
		this.allowArbitrary = allowArbitrary;
	}
	
	/**
	 * Creates an empty, anonymous AC: nothing is required nor allowed
	 */
	public AttributesClass()
	{
		this.name = "";
		this.description = "";
		mandatory = new HashSet<>();
		allowed = new HashSet<>();
		allowArbitrary = false;
	}
	
	public boolean isAllowedDirectly(String type)
	{
		return allowArbitrary || allowed.contains(type);
	}
	
	public boolean isMandatoryDirectly(String type)
	{
		return mandatory.contains(type);
	}

	public Set<String> getAllowed()
	{
		return allowed;
	}

	public void setAllowed(Set<String> allowed)
	{
		this.allowed = new HashSet<String>(allowed);
	}

	public Set<String> getMandatory()
	{
		return mandatory;
	}

	public void setMandatory(Set<String> mandatory)
	{
		this.mandatory = new HashSet<String>(mandatory);
		this.allowed.addAll(mandatory);
	}

	public boolean isAllowArbitrary()
	{
		return allowArbitrary;
	}

	public void setAllowArbitrary(boolean allowArbitrary)
	{
		this.allowArbitrary = allowArbitrary;
	}

	public Set<String> getParentClasses()
	{
		return parentClasses;
	}

	public void setParentClassName(Set<String> parentClasses)
	{
		this.parentClasses = new HashSet<String>(parentClasses);
	}
}
