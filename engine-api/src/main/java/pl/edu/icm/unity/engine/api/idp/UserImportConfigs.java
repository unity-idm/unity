/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp;

import java.util.Objects;
import java.util.Set;

public class UserImportConfigs
{
	public final boolean skip;
	public final Set<UserImportConfig> configs;

	public UserImportConfigs(boolean skip, Set<UserImportConfig> configs)
	{
		this.skip = skip;
		this.configs = configs;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserImportConfigs that = (UserImportConfigs) o;
		return skip == that.skip && Objects.equals(configs, that.configs);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(skip, configs);
	}

	@Override
	public String toString()
	{
		return "UserImportConfigs{" +
				"skip=" + skip +
				", configs=" + configs +
				'}';
	}
}
