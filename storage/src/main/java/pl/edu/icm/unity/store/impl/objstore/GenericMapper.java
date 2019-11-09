/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.objstore;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.store.rdbms.BasicCRUDMapper;

/**
 * Access to Generic.xml operations
 * @author K. Benedyczak
 */
public interface GenericMapper extends BasicCRUDMapper<GenericObjectBean>
{
	void deleteByType(String type);
	
	List<GenericObjectBean> selectObjectsByType(String type);
	Set<String> selectObjectNamesByType(String type);
	Set<String> selectObjectTypes();
	GenericObjectBean selectObjectByNameType(GenericObjectBean nameType);
	long getCountByType(String type);
}
