/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.types.basic.EntityInformation;


/**
 * RDBMS storage of {@link StoredEntity} without cache
 * @author K. Benedyczak
 */
public class EntityRDBMSStore extends GenericRDBMSCRUD<EntityInformation, BaseBean> 
					implements EntityDAO
{
	public EntityRDBMSStore(EntityJsonSerializer jsonSerializer)
	{
		super(EntitiesMapper.class, jsonSerializer, NAME);
	}
	
	@Override
	public long create(EntityInformation obj)
	{
		long ret = super.create(obj);
		obj.setId(ret);
		return ret;
	}
}
