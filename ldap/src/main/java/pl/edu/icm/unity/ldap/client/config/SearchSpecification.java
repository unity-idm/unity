/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client.config;

import pl.edu.icm.unity.ldap.client.LdapUnsafeArgsEscaper;
import pl.edu.icm.unity.ldap.client.config.common.LDAPCommonProperties.SearchScope;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;


/**
 * Specification of an additional attribute query.
 * 
 * @author K. Benedyczak
 */
public class SearchSpecification
{
	private String filter;
	private String baseDN;
	private String attributes;
	private SearchScope scope;
	
	public SearchSpecification(String filter, String baseDN, String attributes, 
			SearchScope scope) throws LDAPException
	{
		createFilter(filter, "test");
		this.filter = filter;
		this.baseDN = baseDN;
		this.attributes = attributes;
		this.scope = scope;
	}

	public SearchSpecification()
	{
		//for binder
		this.scope = SearchScope.sub;
	}
	
	public Filter getFilter(String username) throws LDAPException
	{
		return createFilter(filter, username);
	}
	
	public String getFilter()
	{
		return filter;
	}

	public void setFilter(String filter)
	{
		this.filter = filter;
	}

	public String getBaseDN(String username)
	{
		String sanitizedInput = LdapUnsafeArgsEscaper.escapeLDAPSearchFilter(username);
		return baseDN.replace("{USERNAME}", sanitizedInput);  
	}
	
	public String getBaseDN()
	{
		return baseDN;
	}

	public void setBaseDN(String baseDN)
	{
		this.baseDN = baseDN;
	}

	public String getAttributes()
	{
		return attributes;
	}
	
	public String[] getSplitedAttributes()
	{
		return attributes!= null ? attributes.split("[ ]+") : new String[0];
	}

	public void setAttributes(String attributes)
	{
		this.attributes = attributes;
	}
	
	public SearchScope getScope()
	{
		return scope;
	}

	public void setScope(SearchScope scope)
	{
		this.scope = scope;
	}

	public static Filter createFilter(String filterExp, String username) throws LDAPException
	{
		String sanitizedInput = LdapUnsafeArgsEscaper.escapeLDAPSearchFilter(username);
		String filterStr = filterExp.replace("{USERNAME}", sanitizedInput);  
		return Filter.create(filterStr);
	}
}
