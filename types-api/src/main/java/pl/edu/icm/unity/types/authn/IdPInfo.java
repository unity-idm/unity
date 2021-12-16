/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.authn;

import java.util.Objects;
import java.util.Optional;

import pl.edu.icm.unity.types.I18nString;

public class IdPInfo
{
	public final String id;
	public final Optional<I18nString> displayedName;

	public final Optional<IdpGroup> group;

	public IdPInfo(String id, Optional<I18nString> displayedName, Optional<IdpGroup> group)
	{
		this.id = id;
		this.displayedName = displayedName;
		this.group = group;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(group, id);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdPInfo other = (IdPInfo) obj;
		return Objects.equals(group, other.group) && Objects.equals(id, other.id);
	}

	public static class IdpGroup
	{
		public final String id;
		public final Optional<String> displayedName;

		public IdpGroup(String id, Optional<String> displayedName)
		{
			this.id = id;
			this.displayedName = displayedName;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(id);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			IdpGroup other = (IdpGroup) obj;
			return Objects.equals(id, other.id);
		}

	}
}
