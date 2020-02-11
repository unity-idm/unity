/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import org.apache.http.entity.ContentType;

public class SharedAttributeContent
{
	public final byte[] content;
	public final ContentType type;

	public SharedAttributeContent(byte[] content, ContentType type)
	{
		this.content = content;
		this.type = type;
	}
}
