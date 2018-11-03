/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import java.util.List;

import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.BasicCRUDMapper;

/**
 * Access to entities operations.
 * @author K. Benedyczak
 */
public interface EntitiesMapper extends BasicCRUDMapper<BaseBean>
{
	List<BaseBean> getByGroup(String path);
}
