/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identitytype;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.GenericNamedRDBMSCRUD;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Caching implementation of {@link IdentityTypeDAO}.
 * 
 * @author K. Benedyczak
 */
@Repository(IdentityTypeRDBMSStore.BEAN)
public class IdentityTypeRDBMSStore extends GenericNamedRDBMSCRUD<IdentityType, BaseBean> implements IdentityTypeDAO
{
	public static final String BEAN = DAO_ID + "rdbms";
	
	@Autowired
	public IdentityTypeRDBMSStore(IdentityTypeJsonSerializer jsonSerializer)
	{
		super(IdentityTypesMapper.class, jsonSerializer, "identity type");
	}
}
