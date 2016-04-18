/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.internal;

/**
 * Enumerates available storage engines.
 * @author K. Benedyczak
 */
public enum StorageEngine
{
	/**
	 * The legacy RDBMS only engine.
	 */
	rdbms,

	/**
	 * Hazelcast based engine with RDBMS flushing
	 */
	hz
}
