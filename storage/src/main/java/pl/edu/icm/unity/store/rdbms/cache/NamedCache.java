/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.cache;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.types.NamedObject;

/**
 * Cache of named objects for use with {@link NamedCRUDDAO}
 *  
 * @author K. Benedyczak
 */
public interface NamedCache<T extends NamedObject> extends BasicCache<T>
{
	Optional<Boolean> exists(String id);

	Optional<Map<String, T>> getAllAsMap();

	Optional<T> get(String id);
	
	Optional<Long> getKeyForName(String id);
	
	Optional<Set<String>> getAllNames();

	void storeByName(String id, T element);
}
