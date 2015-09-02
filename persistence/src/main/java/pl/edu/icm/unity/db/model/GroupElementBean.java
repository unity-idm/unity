/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;

/**
 * In DB representation of group's element: member or linked group
 * @author K. Benedyczak
 */
public class GroupElementBean
{
	private long groupId;
	private long elementId;
	private byte[] contents;

	public GroupElementBean()
	{
	}	
	
	public GroupElementBean(long groupId, long elementId)
	{
		this.groupId = groupId;
		this.elementId = elementId;
	}
	public long getGroupId()
	{
		return groupId;
	}
	public void setGroupId(long groupId)
	{
		this.groupId = groupId;
	}
	public long getElementId()
	{
		return elementId;
	}
	public void setElementId(long elementId)
	{
		this.elementId = elementId;
	}
	public byte[] getContents()
	{
		return contents;
	}
	public void setContents(byte[] contents)
	{
		this.contents = contents;
	}
}
