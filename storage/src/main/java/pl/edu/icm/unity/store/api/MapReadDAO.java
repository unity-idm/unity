/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.Map;

/**
 * DAO component: read all as map
 * @author K. Benedyczak
 * @param <T>
 */
public interface MapReadDAO<T>
{
	Map<String, T> getAsMap();
}
