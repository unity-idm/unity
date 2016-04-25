/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.StoredEntity;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.mapper.EntitiesMapper;
import pl.edu.icm.unity.store.rdbms.model.BaseBean;


/**
 * RDBMS storage of {@link StoredEntity}
 * @author K. Benedyczak
 */
@Repository(EntityRDBMSStore.BEAN)
public class EntityRDBMSStore extends GenericRDBMSCRUD<StoredEntity, BaseBean> 
					implements EntityDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public EntityRDBMSStore(EntityJsonSerializer jsonSerializer, StorageLimits limits)
	{
		super(EntitiesMapper.class, jsonSerializer, "entity", limits);
	}
}
