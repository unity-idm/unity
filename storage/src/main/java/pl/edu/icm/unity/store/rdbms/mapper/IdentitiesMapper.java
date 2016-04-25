/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.mapper;

import java.util.List;

import pl.edu.icm.unity.store.rdbms.model.IdentityBean;

/**
 * Access to the Identities.xml operations.
 * @author K. Benedyczak
 */
public interface IdentitiesMapper
{
	void updateIdentityEntity(IdentityBean arg);
	
	void insertIdentity(IdentityBean arg);
	void updateIdentity(IdentityBean arg);
	void deleteIdentity(String cmpVal);
	void deleteAllIdentities();
	IdentityBean getIdentityByName(String name);
	List<IdentityBean> getIdentities();
	List<IdentityBean> getIdentitiesByEntity(long entityId);
}
