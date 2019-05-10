/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.file;

import java.util.Arrays;
import java.util.Date;

import com.google.common.base.Objects;

import pl.edu.icm.unity.types.NamedObject;

/**
 * Contains content of local or remote file and additional info about file -
 * name, owner and timestamp
 * 
 * @author P.Piernik
 *
 */
public class FileData implements NamedObject
{
	public final String name;
	public final String ownerType;
	public final String ownerId;
	public final byte[] contents;
	public final Date lastUpdate;

	public FileData(String name, byte[] contents, Date lastUpdate, String ownerType, String ownerId)
	{
		this.name = name;
		if (contents != null)
		{
			this.contents = Arrays.copyOf(contents, contents.length);
		} else
		{
			this.contents = null;
		}
		this.lastUpdate = lastUpdate;
		this.ownerType = ownerType;
		this.ownerId = ownerId;
	}

	public FileData(String name, byte[] contents, Date lastUpdate)
	{
		this(name, contents, lastUpdate, null, null);
	}

	public boolean equalsContent(FileData other)
	{
		return Arrays.equals(this.contents, other.contents);
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(name, ownerType, ownerId, contents, lastUpdate);
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
				&& Objects.equal(this.lastUpdate, other.lastUpdate)
				&& Arrays.equals(this.contents, other.contents);
	}

	@Override
	public String getName()
	{
		return name;
	}

}
