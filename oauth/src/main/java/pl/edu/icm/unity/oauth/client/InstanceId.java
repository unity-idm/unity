/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.util.UUID;

/**
 * Unique identity token for a single OAuth2Verificator instance.
 * Equality is reference-based (default Object semantics) — two distinct
 * InstanceId objects are never equal even if created at the same time.
 * Used to guard singleton manager entries against removal by a stale
 * verificator when a newer instance has already claimed the same name slot.
 */
public final class InstanceId
{
	private final String id = UUID.randomUUID().toString();

	@Override
	public String toString()
	{
		return "InstanceId[" + id + "]";
	}
}
