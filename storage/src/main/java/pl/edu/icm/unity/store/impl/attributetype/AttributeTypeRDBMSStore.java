/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.rdbms.GenericNamedRDBMSCRUD;
import pl.edu.icm.unity.types.basic.AttributeType;


/**
 * RDBMS storage of {@link AttributeType}
 * @author K. Benedyczak
 */
@Repository(AttributeTypeRDBMSStore.BEAN)
public class AttributeTypeRDBMSStore extends GenericNamedRDBMSCRUD<AttributeType, AttributeTypeBean> 
		implements AttributeTypeDAOInternal
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	AttributeTypeRDBMSStore(AttributeTypeRDBMSSerializer jsonSerializer)
	{
		super(AttributeTypesMapper.class, jsonSerializer, "attribute type");
	}
}
