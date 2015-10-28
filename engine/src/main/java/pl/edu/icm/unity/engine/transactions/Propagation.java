/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.transactions;

/**
 * Describes transaction propagation mode for nested transactions
 * @author K. Benedyczak
 */
public enum Propagation
{
	/**
	 * Transaction is always created, unless one is already present - then it is reused 
	 */
	REQUIRED, 
	
	/**
	 * A separate transaction is created always.
	 */
	REQUIRE_SEPARATE
}
