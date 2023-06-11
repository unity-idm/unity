/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import java.util.Date;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.base.describedObject.NamedObject;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;

/**
 * {@link NamedCRUDDAO} enhanced with update timestamps of objects. 
 * 
 * @author K. Benedyczak
 */
public interface NamedCRUDDAOWithTS<T extends NamedObject> extends NamedCRUDDAO<T>
{
	List<Map.Entry<T, Date>> getAllWithUpdateTimestamps();
	List<Map.Entry<String, Date>> getAllNamesWithUpdateTimestamps();
	Date getUpdateTimestamp(String name);
	void updateTS(String id);
	long createWithTS(T newValue, Date updatTS);
}
