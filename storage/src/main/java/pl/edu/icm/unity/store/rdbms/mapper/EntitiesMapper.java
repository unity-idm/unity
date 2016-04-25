/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.mapper;

import pl.edu.icm.unity.store.rdbms.BasicCRUDMapper;
import pl.edu.icm.unity.store.rdbms.model.BaseBean;

/**
 * Access to entities operations.
 * @author K. Benedyczak
 */
public interface EntitiesMapper extends BasicCRUDMapper<BaseBean>
{
	void createWithKey(BaseBean arg);
}
