/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.upman.groups;

import pl.edu.icm.unity.types.I18nString;

class GroupWithAccessMode
{
	final I18nString name;
	final boolean isOpen;

	GroupWithAccessMode(I18nString name, boolean isPublic)
	{
		this.name = name;
		this.isOpen = isPublic;
	}
}