/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.stdext.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Enumeration representing support image types.
 */
public enum ImageType
{
	JPG("image/jpeg"),
	PNG("image/png"),
	GIF("image/gif");

	private String mimeType;

	private ImageType(String mimeType)
	{
		this.mimeType = mimeType;
	}

	public String getMimeType()
	{
		return mimeType;
	}

	public static String getSupportedMimeTypes(String delimiter)
	{
		return Arrays.asList(values()).stream().
				map(ImageType::getMimeType).
				collect(Collectors.joining(delimiter));
	}

	public String toExt()
	{
		return toString().toLowerCase();
	}

	public static ImageType fromExt(String ext)
	{
		return valueOf(ext.toUpperCase());
	}

	public static ImageType fromMimeType(String mimeType)
	{
		for (ImageType type : values())
		{
			if (type.mimeType.equals(mimeType))
				return type;
		}
		throw new InternalException("Unsupported mimeType: " + mimeType);
	}
}
