/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.rdbms.DBLimit;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.mapper.AttributeTypesMapper;
import pl.edu.icm.unity.store.rdbms.model.AttributeTypeBean;
import pl.edu.icm.unity.types.basic.AttributeType;


/**
 * RDBMS storage of {@link AttributeType}
 * @author K. Benedyczak
 */
@Repository(AttributeTypeRDBMSStore.BEAN)
public class AttributeTypeRDBMSStore extends GenericRDBMSCRUD<AttributeType, AttributeTypeBean> 
					implements AttributeTypeDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public AttributeTypeRDBMSStore(AttributeTypeJsonSerializer jsonSerializer, DBLimit limits)
	{
		super(AttributeTypesMapper.class, jsonSerializer, limits, "attribute type");
	}

	@Override
	protected String getNameId(AttributeType obj)
	{
		return obj.getName();
	}
}
