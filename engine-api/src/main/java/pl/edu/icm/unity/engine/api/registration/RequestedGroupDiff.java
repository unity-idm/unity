/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.registration;

import java.util.Set;

/**
 * Contains group diff information
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

		this.toAdd = toAdd;
		this.toRemove = toRemove;
		this.remain = remain;
	}
}
