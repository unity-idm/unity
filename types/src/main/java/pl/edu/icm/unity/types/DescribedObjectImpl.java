/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;


/**
 * Bean implementation of {@link DescribedObject} interface. Useful for extending
 * @author K. Benedyczak
 */
public class DescribedObjectImpl implements DescribedObject
{
	private String id;
	private String name;
	private String description;
	
	public DescribedObjectImpl()
	{
	}

	public DescribedObjectImpl(String id, String name, String description)
	{
		this.id = id;
		this.name = name;
		this.description = description;
	}
	
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
}
