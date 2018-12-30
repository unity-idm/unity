/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.registration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Group membership change request sliced into three groups: unchanged groups, added groups and removed groups
 * 
 * @author P.Piernik
 *
 */
public class RequestedGroupDiff
{
	public final Set<String> toAdd;
	public final Set<String> toRemove;
	public final Set<String> remain;

	public RequestedGroupDiff(Set<String> toAdd, Set<String> toRemove, Set<String> remain)
	{

		this.toAdd = Collections.unmodifiableSet(toAdd != null ? toAdd : new HashSet<>());
		this.toRemove = Collections.unmodifiableSet(toRemove != null ? toRemove : new HashSet<>());
		this.remain = Collections.unmodifiableSet(remain != null ? remain : new HashSet<>());
	}
}
