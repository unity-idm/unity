/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Generic token. See {@link TokensManagement}.
 * 
 * @author K. Benedyczak
 */
public class Token
{
	private String type;
	private String value;
	private Long owner;
	private Date created;
	private Date expires;
	private byte[] contents;

	public Token(String type, String value, Long owner)
	{
		this.type = type;
		this.value = value;
		this.owner = owner;
	}

	public String getType()
	{
		return type;
	}
	public void setType(String type)
	{
		this.type = type;
	}
	public String getValue()
	{
		return value;
	}
	public void setValue(String value)
	{
		this.value = value;
	}
	public Long getOwner()
	{
		return owner;
	}
	public void setOwner(Long owner)
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
	public byte[] getContents()
	{
		return contents;
	}
	public String getContentsString()
	{
		return new String(contents, StandardCharsets.UTF_8);
	}
	public void setContents(byte[] contents)
	{
		this.contents = contents;
	}
}
