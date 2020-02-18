/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

public class PublicAttributeContent
{
	public final byte[] content;
	public final String mimeType;

	public PublicAttributeContent(byte[] content, String mimeType)
	{
		this.content = content;
		this.mimeType = mimeType;
	}
}
