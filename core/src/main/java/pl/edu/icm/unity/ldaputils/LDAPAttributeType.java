/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldaputils;

import java.util.List;


public class LDAPAttributeType
{
	private String oid;
	private List<String> names;
	private String description;
	private String syntax;
	private boolean obsolete;
	private boolean singleValue;
	private boolean noUserModify;
	private boolean collective;
	private String usage;
	private String superclass;
	private String equality;
	private String ordering;
	private String substring;
	
	
	public boolean isNoUserModify()
	{
		return noUserModify;
	}

	public String getUsage()
	{
		return usage;
	}

	public void setUsage(String usage)
	{
		this.usage = usage;
	}

	public void setNoUserModify(boolean noUserModify)
	{
		this.noUserModify = noUserModify;
	}

	public boolean isCollective()
	{
		return collective;
	}

	public void setCollective(boolean collective)
	{
		this.collective = collective;
	}

	public String getOid()
	{
		return oid;
	}

	public void setOid(String oid)
	{
		this.oid = oid;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public boolean isObsolete()
	{
		return obsolete;
	}

	public void setObsolete(boolean obsolete)
	{
		this.obsolete = obsolete;
	}

	public String getSyntax()
	{
		return syntax;
	}

	public void setSyntax(String syntax)
	{
		this.syntax = syntax;
	}


	public List<String> getNames()
	{
		return names;
	}

	public void setNames(List<String> names)
	{
		this.names = names;
	}

	public boolean isSingleValue()
	{
		return singleValue;
	}

	public void setSingleValue(boolean singleValue)
	{
		this.singleValue = singleValue;
	}

	public String getSuperclass()
	{
		return superclass;
	}

	public void setSuperclass(String superclass)
	{
		this.superclass = superclass;
	}

	public String getEquality()
	{
		return equality;
	}

	public void setEquality(String equality)
	{
		this.equality = equality;
	}

	public String getOrdering()
	{
		return ordering;
	}

	public void setOrdering(String ordering)
	{
		this.ordering = ordering;
	}

	public String getSubstring()
	{
		return substring;
	}

	public void setSubstring(String substring)
	{
		this.substring = substring;
	}
	

	@Override
	public String toString()
	{
		return "LDAPAttributeType [oid=" + oid + ", names=" + names + ", description="
				+ description + ", syntax=" + syntax + ", obsolete=" + obsolete
				+ ", singleValue=" + singleValue + ", noUserModify=" + noUserModify
				+ ", collective=" + collective + ", usage=" + usage
				+ ", superclass=" + superclass + ", equality=" + equality
				+ ", ordering=" + ordering + ", substring=" + substring + "]";
	}

}
