/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

public class AttributesAssertion
{
	protected AttributesManagement attrsMan;
	protected EntityParam entity;

	private AttributesAssertion(AttributesManagement attrsMan, String identityType, String identityValue)
	{
		this(attrsMan, new EntityParam(new IdentityTaV(identityType, identityValue)));
	}
	
	protected AttributesAssertion(AttributesManagement attrsMan, EntityParam entity)
	{
		this.attrsMan = attrsMan;
		this.entity = entity;
	}

	public static AttributesAssertion assertThat(AttributesManagement attrsMan, String identityType,
			String identityValue)
	{
		return new AttributesAssertion(attrsMan, identityType, identityValue);
	}

	public AttributesListAssertion hasAttribute(String attrType, String group)
	{
		try
		{
			Collection<AttributeExt> attrs = attrsMan.getAttributes(entity, group, attrType);
			return new AttributesListAssertion(attrsMan, entity, attrs);
		} catch (Exception e)
		{
			throw new AssertionError(e.getMessage(), e);
		}
	}

}
