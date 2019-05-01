/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.file;

import java.util.Arrays;

import com.google.common.base.Objects;

import pl.edu.icm.unity.types.NamedObject;

/**
 * 
 * @author P.Piernik
 *
 */
public class FileData implements NamedObject
{
	private String name;
	private String ownerType;
	private String ownerId;
	private byte[] contents;

	public FileData()
	{
	}
	
	public FileData(String name, byte[] contents)
	{
		this.name = name;
		this.contents = contents;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getOwnerType()
	{
		return ownerType;
	}

	public void setOwnerType(String ownerType)
	{
		this.ownerType = ownerType;
	}

	public String getOwnerId()
	{
		return ownerId;
	}

	public void setOwnerId(String ownerId)
	{
		this.ownerId = ownerId;
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
		return Objects.hashCode(name, ownerType, ownerId, contents);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final FileData other = (FileData) obj;

		return Objects.equal(this.name, other.name) && Objects.equal(this.ownerType, other.ownerType)
				&& Objects.equal(this.ownerId, other.ownerId)
				&& Arrays.equals(this.contents, other.contents);
	}
}
