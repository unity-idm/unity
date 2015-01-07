/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * Describes an operation to be scheduled on an entity.
 * @author K. Benedyczak
 */
public enum EntityScheduledOperation
{
	FORCED_REMOVAL, REMOVAL_AFTER_GRACE_PERIOD, FORCED_DISABLE
}
