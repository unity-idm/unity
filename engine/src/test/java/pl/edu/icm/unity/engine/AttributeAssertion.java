/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.equalTo;

import org.assertj.core.util.Lists;
import org.hamcrest.MatcherAssert;

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

public class AttributeAssertion extends AttributesAssertion
{
	private AttributeExt attributeExt;

	protected AttributeAssertion(AttributesManagement attrsMan, EntityParam entity, AttributeExt attributeExt)
	{
		super(attrsMan, entity);
		this.attributeExt = attributeExt;
	}

	public AttributesAssertion hasValues(String... values)
	{
		MatcherAssert.assertThat(attributeExt.getValues(), equalTo(Lists.newArrayList(values)));
		return this;
	}

}
