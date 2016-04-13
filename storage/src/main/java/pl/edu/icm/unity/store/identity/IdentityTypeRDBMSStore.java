/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.rdbms.DBLimit;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.mapper.IdentityTypesMapper;
import pl.edu.icm.unity.store.rdbms.model.BaseBean;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Implementation of {@link IdentityTypeDAO}.
 * 
 * @author K. Benedyczak
 */
@Repository
public class IdentityTypeRDBMSStore extends GenericRDBMSCRUD<IdentityType, BaseBean> implements IdentityTypeDAO
{
	@Autowired
	public IdentityTypeRDBMSStore(IdentityTypeJsonSerializer jsonSerializer, DBLimit dbLimits)
	{
		super(IdentityTypesMapper.class, jsonSerializer, dbLimits, "identity type");
	}

	@Override
	protected String getNameId(IdentityType obj)
	{
		return obj.getIdentityTypeProvider().getId();
	}
}
