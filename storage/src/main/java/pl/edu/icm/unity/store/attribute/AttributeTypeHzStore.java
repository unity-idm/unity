/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.attribute;

import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.hz.GenericHzCRUD;
import pl.edu.icm.unity.types.basic.AttributeType;


/**
 * Hazelcast impl of {@link AttributeTypeDAO}
 * @author K. Benedyczak
 */
@Repository
public class AttributeTypeHzStore extends GenericHzCRUD<AttributeType> implements AttributeTypeDAO
{
	private static final String STORE_ID = "attributeTypesMap";
	private static final String NAME = "attribute type";

	public AttributeTypeHzStore()
	{
		super(STORE_ID, NAME);
	}

	@Override
	protected String getKey(AttributeType at)
	{
		return at.getName();
	}
}
