/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

/**
 * Interface to mark DAO that cache values in memory. Expose method to invalidate cache.
 * 
 * @author R. Ledzinski
 */
public interface CachingDAO
{
	void invalidateCache();
}
