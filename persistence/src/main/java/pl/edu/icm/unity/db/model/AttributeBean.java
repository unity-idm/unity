/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;

/**
 * In DB attribute representation. It contains also syntax id and name which are stored in the 
 * table with attribute types.
 * @author K. Benedyczak
 */
public class AttributeBean
{
	private long id;
	private long typeId;
	private long groupId;
	private long entityId;
	private byte[] values;

	private String name;
	private String valueSyntaxId;
	
	public long getId()
	{
		return id;
	}
	public void setId(long id)
	{
		this.id = id;
	}
	public long getGroupId()
	{
		return groupId;
	}
	public void setGroupId(long groupId)
	{
		this.groupId = groupId;
	}
	public long getEntityId()
	{
		return entityId;
	}
	public void setEntityId(long entityId)
	{
		this.entityId = entityId;
	}
	public byte[] getValues()
	{
		return values;
	}
	public void setValues(byte[] values)
	{
		this.values = values;
	}
	public long getTypeId()
	{
		return typeId;
	}
	public void setTypeId(long typeId)
	{
		this.typeId = typeId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getValueSyntaxId()
	{
		return valueSyntaxId;
	}
	public void setValueSyntaxId(String valueSyntaxId)
	{
		this.valueSyntaxId = valueSyntaxId;
	}
}
