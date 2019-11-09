/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.objstore;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.store.api.BasicCRUDDAO;


/**
 * Interface allowing to manipulate generic content tables. That this interface is 
 * internal, not in the storage-api module as it is not used directly: this functionality is
 * used internally by object store higher level DAOs.
 * @author K. Benedyczak
 */
public interface ObjectStoreDAO extends BasicCRUDDAO<GenericObjectBean>
{
	String DAO_ID = "ObjectStoreDAO";
	String NAME = "object";

	Set<String> getNamesOfType(String type);
	List<GenericObjectBean> getObjectsOfType(String type);
	/**
	 * Note - this method contrary to other DAOs may return null
	 * @param name
	 * @param type
	 * @return null if not found, otherwise the object
	 */
	GenericObjectBean getObjectByNameType(String name, String type);
	Set<String> getObjectTypes();
	void removeObject(String name, String type);
	void removeObjectsByType(String type);
	void updateObject(String name, String type, GenericObjectBean updated); 
	long getCountByType(String type);
}



