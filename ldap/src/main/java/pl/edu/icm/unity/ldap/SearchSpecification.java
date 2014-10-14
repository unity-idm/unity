/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;


/**
 * Specification of an additional attribute query.
 * 
 * @author K. Benedyczak
 */
public class SearchSpecification
{
	private String filterExp;
	private String baseDN;
	private String[] attributes;
	
	public SearchSpecification(String filter, String baseDN, String[] attributes) throws LDAPException
	{
		super();
		createFilter(filter, "test");
		this.filterExp = filter;
		this.baseDN = baseDN;
		this.attributes = attributes;
	}

	public SearchSpecification()
	{
	}
	
	public Filter getFilter(String username) throws LDAPException
	{
		return createFilter(filterExp, username);
	}

	public void setFilter(String filter) throws LDAPException
	{
		createFilter(filter, "test");
		this.filterExp = filter;
	}

	public String getBaseDN()
	{
		return baseDN;
	}

	public void setBaseDN(String baseDN)
	{
		this.baseDN = baseDN;
	}

	public String[] getAttributes()
	{
		return attributes;
	}

	public void setAttributes(String[] attributes)
	{
		this.attributes = attributes;
	}
	
	private static Filter createFilter(String filterExp, String username) throws LDAPException
	{
		String sanitizedInput = LdapUnsafeArgsEscaper.escapeLDAPSearchFilter(username);
		String filterStr = filterExp.replace("{USERNAME}", sanitizedInput);  
		return Filter.create(filterStr);
	}
}
