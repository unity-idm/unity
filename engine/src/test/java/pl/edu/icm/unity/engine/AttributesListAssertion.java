/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Collection;
import java.util.List;

import org.hamcrest.MatcherAssert;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

public class AttributesListAssertion extends AttributesAssertion
{
	private List<AttributeExt> attrs;

	AttributesListAssertion(AttributesManagement attrsMan, EntityParam entity, Collection<AttributeExt> attrs)
	{
		super(attrsMan, entity);
		this.attrs = Lists.newArrayList(attrs);
	}

	public AttributesListAssertion count(int expectedNumberOfAttrs)
	{
		MatcherAssert.assertThat(attrs.size(), equalTo(expectedNumberOfAttrs));
		return this;
	}

	public AttributeAssertion attr(int idx)
	{
		if (idx > attrs.size())
			throw new AssertionError("Given index to get attribute exceeds number of "
					+ "available attributes " + attrs.size());
		return new AttributeAssertion(attrsMan, entity, attrs.get(idx));
	}
}
