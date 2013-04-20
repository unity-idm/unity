/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldaputils;

import java.util.List;

public class LDAPObjectClass
{
	private String oid;
	private List<String> names;
	//private String description;
	private List<String> superclasses;
	//private boolean obsolete;
	private List<String> requiredAttributes;
	private List<String> optionalAttributes;
	
	
	public String getOid()
	{
		return oid;
	}
	public void setOid(String oid)
	{
		this.oid = oid;
	}
	public List<String> getSuperclasses()
	{
		return superclasses;
	}
	public void setSuperclasses(List<String> superclasses)
	{
		this.superclasses = superclasses;
	}
	public List<String> getRequiredAttributes()
	{
		return requiredAttributes;
	}
	public void setRequiredAttributes(List<String> requiredAttributes)
	{
		this.requiredAttributes = requiredAttributes;
	}
	public List<String> getOptionalAttributes()
	{
		return optionalAttributes;
	}
	public void setOptionalAttributes(List<String> optionalAttributes)
	{
		this.optionalAttributes = optionalAttributes;
	}
	public List<String> getNames()
	{
		return names;
	}
	public void setNames(List<String> names)
	{
		this.names = names;
	}
}
