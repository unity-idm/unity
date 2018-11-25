/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.delegatedgroup;

import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;

/**
 * Holds information about delegated group.
 * 
 * @author P.Piernik
 *
 */
public class DelegatedGroup
{
	public final String path;
	public final GroupDelegationConfiguration delegationConfiguration;
	public final boolean open;
	public final String displayedName;

	public DelegatedGroup(String path, GroupDelegationConfiguration delegationConfiguration,
			boolean open, String displayedName)
	{
		this.path = path;
		this.delegationConfiguration = delegationConfiguration;
		this.open = open;
		this.displayedName = displayedName;
	}
}
