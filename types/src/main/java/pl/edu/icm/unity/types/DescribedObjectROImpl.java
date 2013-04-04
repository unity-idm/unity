/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;


/**
 * Read only implementation of {@link DescribedObject} interface. Useful for extending
 * @author K. Benedyczak
 */
public class DescribedObjectROImpl implements DescribedObject
{
	protected String name;
	protected String description;
	
	protected DescribedObjectROImpl()
	{
	}
	
	public DescribedObjectROImpl(String name, String description)
	{
		this.name = name;
		this.description = description;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public String toString()
	{
		return "DescribedObject [name=" + name + ", description=" + description + "]";
	}
	
	
}
