/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.objstore;

import java.util.Date;

import pl.edu.icm.unity.store.rdbms.BaseBean;

public class GenericObjectBean extends BaseBean
{
	private String type;
	private Date lastUpdate;

	public GenericObjectBean()
	{
		super();
	}
	public GenericObjectBean(String name, String type)
	{
		super();
		setName(name);
		this.type = type;
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
	
	public Date getLastUpdate()
	{
		return lastUpdate;
	}
	public void setLastUpdate(Date lastUpdate)

	{
		this.lastUpdate = lastUpdate;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((lastUpdate == null) ? 0 : lastUpdate.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericObjectBean other = (GenericObjectBean) obj;
		if (lastUpdate == null)
		{
			if (other.lastUpdate != null)
				return false;
		} else if (!lastUpdate.equals(other.lastUpdate))
			return false;
		if (type == null)
		{
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}
