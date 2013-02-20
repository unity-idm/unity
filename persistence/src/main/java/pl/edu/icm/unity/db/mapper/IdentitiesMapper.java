/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.db.mapper;

import java.util.List;

import pl.edu.icm.unity.db.model.BaseBean;

/**
 * Access to the Identities.xml operations.
 * @author K. Benedyczak
 */
public interface IdentitiesMapper
{
	public void insertIdentityType(BaseBean arg);
	public void updateIdentityType(BaseBean arg);
	public List<BaseBean> getIdentityTypes();
}
