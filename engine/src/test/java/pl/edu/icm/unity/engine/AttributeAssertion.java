/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;



import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.engine.api.AttributesManagement;

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
		Assertions.assertThat(attributeExt.getValues()).isEqualTo(Lists.newArrayList(values));
		return this;
	}

}
