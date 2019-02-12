/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.util.Objects;

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

	public DelegatedGroup(String path, GroupDelegationConfiguration delegationConfiguration, boolean open,
			String displayedName)
	{
		this.path = path;
		this.delegationConfiguration = delegationConfiguration;
		this.open = open;
		this.displayedName = displayedName;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.path, this.delegationConfiguration, this.open, this.displayedName);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final DelegatedGroup other = (DelegatedGroup) obj;
		return Objects.equals(this.path, other.path)
				&& Objects.equals(this.delegationConfiguration, other.delegationConfiguration)
				&& Objects.equals(this.open, other.open)
				&& Objects.equals(this.displayedName, other.displayedName);

	}
}
