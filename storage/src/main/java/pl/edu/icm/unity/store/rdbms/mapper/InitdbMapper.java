/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.mapper;

import pl.edu.icm.unity.store.rdbms.model.DBLimitsBean;


/**
 * Access to the initialization operations
 * @author K. Benedyczak
 */
public interface InitdbMapper
{
	public DBLimitsBean getDBLimits();
}
