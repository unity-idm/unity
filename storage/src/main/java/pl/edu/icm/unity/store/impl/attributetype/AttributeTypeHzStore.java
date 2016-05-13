/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.hz.GenericNamedHzCRUD;
import pl.edu.icm.unity.types.basic.AttributeType;


/**
 * Hazelcast impl of {@link AttributeTypeDAO}
 * @author K. Benedyczak
 */
@Repository(AttributeTypeHzStore.STORE_ID)
public class AttributeTypeHzStore extends GenericNamedHzCRUD<AttributeType> implements AttributeTypeDAOInternal
{
	public static final String STORE_ID = DAO_ID + "hz";
	private static final String NAME = "attribute type";

	@Autowired
	public AttributeTypeHzStore(AttributeTypeRDBMSStore rdbmsStore)
	{
		super(STORE_ID, NAME, AttributeTypeRDBMSStore.BEAN, rdbmsStore);
	}
}
