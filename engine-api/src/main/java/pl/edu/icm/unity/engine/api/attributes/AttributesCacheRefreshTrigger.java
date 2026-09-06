/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

/**
 * Allows to wake up the background process which regenerates the materialized (effective) attributes cache
 * for entities/groups marked as having a pending update. Implementations process pending updates
 * periodically anyway, so calling {@link #wakeUp()} is only an optimization triggering a prompt refresh.
 */
public interface AttributesCacheRefreshTrigger
{
	void wakeUp();
}
