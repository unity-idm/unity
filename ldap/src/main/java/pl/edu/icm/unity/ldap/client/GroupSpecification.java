/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

/**
 * Configuration class, specifies information required to find members in an LDAP group.
 *  
 * @author K. Benedyczak
 */
public class GroupSpecification
{
	private String objectClass;
	private String memberAttribute;
	private String groupNameAttribute;
	private String matchByMemberAttribute;

	public GroupSpecification()
	{
	}
	
	public GroupSpecification(String objectClass, String memberAttribute,
			String groupNameAttribute, String matchByMemberAttribute)
	{
		this.objectClass = objectClass;
		this.memberAttribute = memberAttribute;
		this.groupNameAttribute = groupNameAttribute;
		this.matchByMemberAttribute = matchByMemberAttribute;
	}

	public String getObjectClass()
	{
		return objectClass;
	}
	public void setObjectClass(String objectClass)
	{
		this.objectClass = objectClass;
	}
	public String getMemberAttribute()
	{
		return memberAttribute;
	}
	public void setMemberAttribute(String memberAttribute)
	{
		this.memberAttribute = memberAttribute;
	}
	public String getGroupNameAttribute()
	{
		return groupNameAttribute;
	}
	public void setGroupNameAttribute(String groupNameAttribute)
	{
		this.groupNameAttribute = groupNameAttribute;
	}
	public String getMatchByMemberAttribute()
	{
		return matchByMemberAttribute;
	}
	public void setMatchByMemberAttribute(String matchByMemberAttribute)
	{
		this.matchByMemberAttribute = matchByMemberAttribute;
	}
}
