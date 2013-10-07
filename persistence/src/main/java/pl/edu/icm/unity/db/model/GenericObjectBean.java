/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;

import java.util.Date;

public class GenericObjectBean extends BaseBean
{
	private String type;
	private String subType;
	private Date lastUpdate;

	public GenericObjectBean()
	{
		super();
	}
	public GenericObjectBean(String name, byte[] contents, String type)
	{
		super(name, contents);
		this.type = type;
	}
	public GenericObjectBean(String name, byte[] contents, String type, String subType)
	{
		this(name, contents, type);
		this.subType = subType;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}
	
	public String getSubType()
	{
		return subType;
	}
	
	public void setSubType(String subType)
	{
		this.subType = subType;
	}
	public Date getLastUpdate()
	{
		return lastUpdate;
	}
	public void setLastUpdate(Date lastUpdate)
	{
		this.lastUpdate = lastUpdate;
	}
}
