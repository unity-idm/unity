/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;

public class ImageConfiguration
{
	private int maxWidth = Integer.MAX_VALUE;
	private int maxHeight = Integer.MAX_VALUE;
	private int maxSize = 1024 * 1024;

	public JsonNode getSerializedConfiguration()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("maxWidth", getMaxWidth());
		main.put("maxHeight", getMaxHeight());
		main.put("maxSize", getMaxSize());
		return main;
	}

	public void setSerializedConfiguration(JsonNode jsonN)
	{
		maxWidth = jsonN.get("maxWidth").asInt();
		maxHeight = jsonN.get("maxHeight").asInt();
		maxSize = jsonN.get("maxSize").asInt();
	}

	public int getMaxWidth()
	{
		return maxWidth;
	}

	public int getMaxHeight()
	{
		return maxHeight;
	}

	public int getMaxSize()
	{
		return maxSize;
	}

	public void setMaxSize(int max) throws WrongArgumentException
	{
		if (max <= 0)
			throw new WrongArgumentException("Maximum size must be positive number");
		this.maxSize = max;
	}

	public void setMaxWidth(int maxWidth) throws WrongArgumentException
	{
		if (maxWidth <= 0)
			throw new WrongArgumentException("Maximum width must be positive number");
		this.maxWidth = maxWidth;
	}

	public void setMaxHeight(int maxHeight) throws WrongArgumentException
	{
		if (maxHeight <= 0)
			throw new WrongArgumentException("Maximum height must be positive number");
		this.maxHeight = maxHeight;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(maxWidth, maxHeight, maxSize);
	}

	@Override
	public boolean equals(Object object)
	{
		if (object instanceof ImageConfiguration)
		{
			ImageConfiguration that = (ImageConfiguration) object;
			return this.maxWidth == that.maxWidth && this.maxHeight == that.maxHeight && this.maxSize == that.maxSize;
		}
		return false;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this).add("maxWidth", maxWidth).add("maxHeight", maxHeight)
				.add("maxSize", maxSize).toString();
	}
}
