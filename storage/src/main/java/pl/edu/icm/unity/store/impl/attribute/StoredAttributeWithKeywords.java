/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Objects;

import com.google.common.base.MoreObjects;

import pl.edu.icm.unity.store.types.StoredAttribute;

class StoredAttributeWithKeywords
{
	private final List<String> keywords;
	private final StoredAttribute storedAttribute;

	StoredAttributeWithKeywords(StoredAttribute storedAttribute, List<String> keywords)
	{
		this.storedAttribute = storedAttribute;
		this.keywords = keywords == null ? emptyList() : keywords;
	}

	List<String> getKeywords()
	{
		return keywords;
	}

	StoredAttribute getStoredAttribute()
	{
		return storedAttribute;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(keywords, storedAttribute);
	}

	@Override
	public boolean equals(Object object)
	{
		if (object instanceof StoredAttributeWithKeywords)
		{
			StoredAttributeWithKeywords that = (StoredAttributeWithKeywords) object;
			return Objects.equals(this.keywords, that.keywords)
					&& Objects.equals(this.storedAttribute, that.storedAttribute);
		}
		return false;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this).add("keywords", keywords).add("storedAttribute", storedAttribute)
				.toString();
	}

}
