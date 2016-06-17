/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.token;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Generic token.
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

	/**
	 * For JSON deserialization
	 */
	protected Token()
	{
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
	@JsonIgnore
	public boolean isExpired()
	{
		return expires.after(new Date());
	}
	public void setExpires(Date expires)
	{
		this.expires = expires;
	}
	public byte[] getContents()
	{
		return contents;
	}
	@JsonIgnore
	public String getContentsString()
	{
		return new String(contents, StandardCharsets.UTF_8);
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
		result = prime * result + Arrays.hashCode(contents);
		result = prime * result + ((created == null) ? 0 : created.hashCode());
		result = prime * result + ((expires == null) ? 0 : expires.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		Token other = (Token) obj;
		if (!Arrays.equals(contents, other.contents))
			return false;
		if (created == null)
		{
			if (other.created != null)
				return false;
		} else if (!created.equals(other.created))
			return false;
		if (expires == null)
		{
			if (other.expires != null)
				return false;
		} else if (!expires.equals(other.expires))
			return false;
		if (owner == null)
		{
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (type == null)
		{
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (value == null)
		{
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "Token [type=" + type + ", value=" + value + ", owner=" + owner + "]";
	}
}
