/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identitytype;

import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.GenericNamedRDBMSCRUD;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Implementation of {@link IdentityTypeDAO} without cacheing
 * 
 * @author K. Benedyczak
 */
class IdentityTypeRDBMSStoreInt extends GenericNamedRDBMSCRUD<IdentityType, BaseBean> implements IdentityTypeDAO
{
	public IdentityTypeRDBMSStoreInt(IdentityTypeJsonSerializer jsonSerializer)
	{
		super(IdentityTypesMapper.class, jsonSerializer, "identity type");
	}
}
