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
	private Long id;
	private Long typeId;
	private Long groupId;
	private Long entityId;
	private byte[] values;

	private String name;
	private String valueSyntaxId;
	
	public Long getId()
	{
		return id;
	}
	public void setId(Long id)
	{
		this.id = id;
	}
	public Long getGroupId()
	{
		return groupId;
	}
	public void setGroupId(Long groupId)
	{
		this.groupId = groupId;
	}
	public Long getEntityId()
	{
		return entityId;
	}
	public void setEntityId(Long entityId)
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
	public Long getTypeId()
	{
		return typeId;
	}
	public void setTypeId(Long typeId)
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
