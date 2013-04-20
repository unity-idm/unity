/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldaputils;

import java.util.ArrayList;
import java.util.List;

/**
 * Ldap schema parser output - attribute types and object classes.
 * @author K. Benedyczak
 */
public class LDAPSchema
{
	private List<LDAPAttributeType> attributeTypes = new ArrayList<LDAPAttributeType>();
	private List<LDAPObjectClass> objectClasses = new ArrayList<LDAPObjectClass>();
	
	public List<LDAPAttributeType> getAttributeTypes()
	{
		return attributeTypes;
	}
	public void addAttributeType(LDAPAttributeType attributeType)
	{
		this.attributeTypes.add(attributeType);
	}
	
	public List<LDAPObjectClass> getObjectClasses()
	{
		return objectClasses;
	}
	
	public void addObjectClass(LDAPObjectClass objectClass)
	{
		this.objectClasses.add(objectClass);
	}
}
