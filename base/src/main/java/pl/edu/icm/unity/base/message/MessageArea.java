/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.message;

import java.util.Objects;

public class MessageArea
{
	public final String name;
	public final String displayedNameKey;
	public final boolean isEditable;

	public MessageArea(String name, String displayedNameKey, boolean isEditable)
	{
		this.name = name;
		this.displayedNameKey = displayedNameKey;
		this.isEditable = isEditable;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof MessageArea))
			return false;
		MessageArea castOther = (MessageArea) other;

		return Objects.equals(this.name, castOther.name)
				&& Objects.equals(this.displayedNameKey, castOther.displayedNameKey)
				&& Objects.equals(this.isEditable, castOther.isEditable);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, displayedNameKey, isEditable);
	}
}
