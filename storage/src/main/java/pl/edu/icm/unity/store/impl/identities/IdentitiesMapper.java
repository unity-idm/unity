/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Param;

import pl.edu.icm.unity.store.rdbms.NamedCRUDMapper;

/**
 * Access to the Identities.xml operations.
 * @author K. Benedyczak
 */
public interface IdentitiesMapper extends NamedCRUDMapper<IdentityBean>
{
	List<IdentityBean> getByEntity(long entityId);
	List<IdentityBean> getByGroup(String group);
	long getCountByType(List<String> types);
	Set<Long> getIdByTypeAndNames(@Param("identityType") String type, @Param("names") List<String> names);
}
