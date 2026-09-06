/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.types;

/**
 * Identifies an entity in a group, without any additional payload.
 */
public record EntityInGroup(long entityId, String group)
{
}
