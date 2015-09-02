/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in;


/**
 * Mapped group: name and flag stating whether it should be created if missing.
 * @author K. Benedyczak
 */
public class MappedGroup
{
	private String group;
	private GroupEffectMode createIfMissing;
	private String idp;
	private String profile;
	
	public MappedGroup(String group, GroupEffectMode createIfMissing, String idp, String profile)
	{
		super();
		this.group = group;
		this.createIfMissing = createIfMissing;
		this.idp = idp;
		this.profile = profile;
	}
	
	public String getGroup()
	{
		return group;
	}
	public void setGroup(String group)
	{
		this.group = group;
	}
	public GroupEffectMode getCreateIfMissing()
	{
		return createIfMissing;
	}
	public void setCreateIfMissing(GroupEffectMode createIfMissing)
	{
		this.createIfMissing = createIfMissing;
	}
        public String getIdp()
	{
		return idp;
	}
	public String getProfile()
	{
		return profile;
	}

	public String toString()
        {
	    return group;
        }
}
