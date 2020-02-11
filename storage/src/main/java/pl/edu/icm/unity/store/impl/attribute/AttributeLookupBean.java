/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.Objects;

import com.google.common.base.MoreObjects;

public class AttributeLookupBean
{
	private Long id;
	private String keyword;
	private Long attributeId;

	public AttributeLookupBean(Long id, String keyword, Long attributeId)
	{
		this.id = id;
		this.keyword = keyword;
		this.attributeId = attributeId;
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public String getkeyword()
	{
		return keyword;
	}

	public void setkeyword(String keyword)
	{
		this.keyword = keyword;
	}

	public Long getAttributeId()
	{
		return attributeId;
	}

	public void setAttributeId(Long attributeId)
	{
		this.attributeId = attributeId;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, keyword, attributeId);
	}

	@Override
	public boolean equals(Object object)
	{
		if (object instanceof AttributeLookupBean)
		{
			AttributeLookupBean that = (AttributeLookupBean) object;
			return Objects.equals(this.id, that.id) && Objects.equals(this.keyword, that.keyword)
					&& Objects.equals(this.attributeId, that.attributeId);
		}
		return false;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this).add("id", id).add("keyword", keyword).add("attributeId", attributeId)
				.toString();
	}

}
