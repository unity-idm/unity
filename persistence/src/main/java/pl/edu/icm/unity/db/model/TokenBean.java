/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;

import java.util.Date;

public class TokenBean extends BaseBean
{
	private String type;
	private long owner;
	private Date created;
	private Date expires;

	public TokenBean()
	{
		super();
	}
	public TokenBean(String name, String type)
	{
		super(name, null);
		this.type = type;
	}
	public TokenBean(String name, byte[] contents, String type, long owner, Date created)
	{
		super(name, contents);
		this.type = type;
		this.owner = owner;
		this.created = created;
	}
	
	public TokenBean(String name, byte[] contents, String type, Date created)
	{
		super(name, contents);
		this.type = type;
		this.created = created;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}
	public long getOwner()
	{
		return owner;
	}
	public void setOwner(long owner)
	{
		this.owner = owner;
	}
	public Date getCreated()
	{
		return created;
	}
	public void setCreated(Date created)
	{
		this.created = created;
	}
	public Date getExpires()
	{
		return expires;
	}
	public void setExpires(Date expires)
	{
		this.expires = expires;
	}
	
	public boolean isExpired()
	{
		return expires != null ? new Date().after(expires) : false;
	}
}
