/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.capacityLimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.capacity_limit.CapacityLimit;
import pl.edu.icm.unity.store.api.generic.CapacityLimitDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;

/**
 * Easy access to {@link CapacityLimit} storage.
 * 
 * @author P.Piernik
 *
 */
@Component
public class CapacityLimitDBImpl extends GenericObjectsDAOImpl<CapacityLimit> implements CapacityLimitDB
{
	@Autowired
	public CapacityLimitDBImpl(CapacityLimitHandler handler, ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, CapacityLimit.class, "capacity limit");
	}
}
