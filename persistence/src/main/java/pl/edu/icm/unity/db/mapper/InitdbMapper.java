/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.mapper;

import pl.edu.icm.unity.db.model.DBLimits;


/**
 * Access to the initialization operations
 * @author K. Benedyczak
 */
public interface InitdbMapper
{
	public DBLimits getDBLimits();
}
