/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic representation of a remotely obtained attribute.  
 * @author K. Benedyczak
 */
public class RemoteAttribute extends RemoteInformationBase
{
	private List<Object> values;
	
	public RemoteAttribute(String name, Object... values)
	{
		super(name);
		this.values = new ArrayList<>();
		for (Object value: values)
			this.values.add(value);
	}

	public List<Object> getValues()
	{
		return values;
	}

	public void setValues(List<Object> values)
	{
		this.values = values;
	}
	
	@Override
	public String toString()
	{
		return getName() + ": " + values;
	}
}
