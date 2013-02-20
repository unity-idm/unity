/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;


/**
 * In DB representation of basic data which is present in the most of tables.
 * @author K. Benedyczak
 */
public class BaseBean
{
	private Integer id;
	private String name;
	private byte[] contents;
	
	public Integer getId()
	{
		return id;
	}
	public void setId(Integer id)
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
	public byte[] getContents()
	{
		return contents;
	}
	public void setContents(byte[] contents)
	{
		this.contents = contents;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BaseBean other = (BaseBean) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
