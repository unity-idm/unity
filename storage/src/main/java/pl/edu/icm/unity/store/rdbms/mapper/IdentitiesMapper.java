/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.mapper;

import java.util.List;

import pl.edu.icm.unity.store.rdbms.NamedCRUDMapper;
import pl.edu.icm.unity.store.rdbms.model.IdentityBean;

/**
 * Access to the Identities.xml operations.
 * @author K. Benedyczak
 */
public interface IdentitiesMapper extends NamedCRUDMapper<IdentityBean>
{
	List<IdentityBean> getByEntity(long entityId);
}
