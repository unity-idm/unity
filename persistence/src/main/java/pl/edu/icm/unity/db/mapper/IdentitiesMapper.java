/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.db.mapper;

import java.util.List;

import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.IdentityBean;

/**
 * Access to the Identities.xml operations.
 * @author K. Benedyczak
 */
public interface IdentitiesMapper
{
	void insertIdentityType(BaseBean arg);
	void updateIdentityType(BaseBean arg);
	List<BaseBean> getIdentityTypes();
	BaseBean getIdentityTypeByName(String name);
	BaseBean getIdentityTypeById(long id);
	
	void insertEntity(BaseBean arg);
	void insertEntityWithId(BaseBean arg);
	List<BaseBean> getEntities();	
	BaseBean getEntityById(long id);
	void updateEntity(BaseBean arg);
	void deleteEntity(long id);
	
	void updateIdentityEntity(IdentityBean arg);
	
	void insertIdentity(IdentityBean arg);
	void updateIdentity(IdentityBean arg);
	void deleteIdentity(String cmpVal);
	void deleteAllIdentities();
	IdentityBean getIdentityByName(String name);
	List<IdentityBean> getIdentities();
	List<IdentityBean> getIdentitiesByEntity(long entityId);
}
