/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.describedObject;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Bean implementation of {@link DescribedObject} interface. Useful for extending
 * @author K. Benedyczak
 */
public class DescribedObjectImpl extends DescribedObjectROImpl
{
	public DescribedObjectImpl()
	{
	}

	public DescribedObjectImpl(String name, String description)
	{
		super(name, description);
	}
	
	public DescribedObjectImpl(ObjectNode root)
	{
		super(root);
	}
	
	public void setName(String name)
	{
		this.name = name;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
}
