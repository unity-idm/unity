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
 * <p>
 * Attribute class can have a parent. Then all mandatory and allowed attribute types of the parent are
 * added to those defined locally. Allowance of arbitrary attributes set in the child class always overwrite
 * whatever is set in the parent.
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
	private String parentClassName;
	
	
	public AttributesClass(String name, String description, Set<String> allowed, Set<String> mandatory, 
			boolean allowArbitrary, String parentClassId)
	{
		super(name, description);
		this.parentClassName = parentClassId;
		this.allowed = new HashSet<String>(allowed);
		this.mandatory = new HashSet<String>(mandatory);
		this.allowed.addAll(mandatory);
		this.allowArbitrary = allowArbitrary;
	}
	
	public AttributesClass()
	{
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
		this.allowed = allowed;
	}

	public Set<String> getMandatory()
	{
		return mandatory;
	}

	public void setMandatory(Set<String> mandatory)
	{
		this.mandatory = mandatory;
	}

	public boolean isAllowArbitrary()
	{
		return allowArbitrary;
	}

	public void setAllowArbitrary(boolean allowArbitrary)
	{
		this.allowArbitrary = allowArbitrary;
	}

	public String getParentClassName()
	{
		return parentClassName;
	}

	public void setParentClassName(String parentClassName)
	{
		this.parentClassName = parentClassName;
	}
}
