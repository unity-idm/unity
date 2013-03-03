/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;

public class GenericObjectBean extends BaseBean
{
	private String type;

	public GenericObjectBean()
	{
		super();
	}
	public GenericObjectBean(String name, byte[] contents, String type)
	{
		super(name, contents);
		this.type = type;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}
}
