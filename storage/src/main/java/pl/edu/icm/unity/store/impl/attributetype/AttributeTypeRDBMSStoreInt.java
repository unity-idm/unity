/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import pl.edu.icm.unity.store.rdbms.GenericNamedRDBMSCRUD;
import pl.edu.icm.unity.types.basic.AttributeType;


/**
 * RDBMS storage of {@link AttributeType}, basic impl without cache
 * @author K. Benedyczak
 */
class AttributeTypeRDBMSStoreInt extends GenericNamedRDBMSCRUD<AttributeType, AttributeTypeBean> 
					implements AttributeTypeDAOInternal
{
	AttributeTypeRDBMSStoreInt(AttributeTypeRDBMSSerializer jsonSerializer)
	{
		super(AttributeTypesMapper.class, jsonSerializer, "attribute type");
	}
}
