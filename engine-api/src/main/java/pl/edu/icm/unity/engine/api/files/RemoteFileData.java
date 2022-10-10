/**********************************************************************
 *                     Copyright (c) 2022, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.engine.api.files;

import java.util.Date;
import java.util.Objects;

import pl.edu.icm.unity.base.file.FileData;

public class RemoteFileData extends FileData
{
	public final String mimeType;

	public RemoteFileData(String name, byte[] contents, Date lastUpdate, String mimeType)
	{
		super(name, contents, lastUpdate);
		this.mimeType = mimeType;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(mimeType);
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
		RemoteFileData other = (RemoteFileData) obj;
		return Objects.equals(mimeType, other.mimeType);
	}
}
