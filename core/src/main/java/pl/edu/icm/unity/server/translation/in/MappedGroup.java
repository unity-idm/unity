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
	
	public MappedGroup(String group, GroupEffectMode createIfMissing)
	{
		super();
		this.group = group;
		this.createIfMissing = createIfMissing;
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
}
