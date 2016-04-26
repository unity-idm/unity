/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

/**
 * All Hazelcast based DAOs implement this interface. It is used
 * to populate data from persistent store upon startup.
 * @author K. Benedyczak
 */
public interface HzDAO
{
	void populateFromRDBMS();
}
