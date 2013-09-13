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
	public void insertIdentityType(BaseBean arg);
	public void updateIdentityType(BaseBean arg);
	public List<BaseBean> getIdentityTypes();
	public BaseBean getIdentityTypeByName(String name);
	public BaseBean getIdentityTypeById(long id);
	
	public void insertEntity(BaseBean arg);
	public void insertEntityWithId(BaseBean arg);
	public List<BaseBean> getEntities();	
	public BaseBean getEntityById(long id);
	public void updateEntity(BaseBean arg);
	public void deleteEntity(long id);
	
	public void insertIdentity(IdentityBean arg);
	public void updateIdentity(IdentityBean arg);
	public void deleteIdentity(String cmpVal);
	public IdentityBean getIdentityByName(String name);
	public List<IdentityBean> getIdentities();
	public List<IdentityBean> getIdentitiesByEntity(long entityId);
}
