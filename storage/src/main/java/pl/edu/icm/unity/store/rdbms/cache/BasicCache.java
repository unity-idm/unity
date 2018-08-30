/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.cache;

import java.util.List;
import java.util.Optional;

import pl.edu.icm.unity.store.api.BasicCRUDDAO;

/**
 * Basic cache for use with {@link BasicCRUDDAO}
 *  
 * @author K. Benedyczak
 */
public interface BasicCache<T>
{
	void configure(int ttl, int max);
	
	void flushWithEvent();

	void flushWithoutEvent();
	
	Optional<T> getByKey(long id);

	Optional<List<T>> getAll();

	void storeById(long id, T element);
	
	void storeAll(List<T> elements);
	
	void setFlushListener(Runnable flushCallback);
}
