/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.layout.configuration;

import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.base.i18n.I18nString;

/**
 * Stores information about authentication layout configuration
 * 
 * @author P.Piernik
 *
 */
public class AuthnLayoutConfiguration
{
	public final List<AuthnLayoutColumnConfiguration> columns;
	public final List<I18nString> separators;

	public AuthnLayoutConfiguration(List<AuthnLayoutColumnConfiguration> columns, List<I18nString> separators)
	{
		this.columns = Collections.unmodifiableList(columns);
		this.separators = Collections.unmodifiableList(separators);
	}

	@Override
	public int hashCode()
	{
		return java.util.Objects.hash(columns, separators);
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
		AuthnLayoutConfiguration other = (AuthnLayoutConfiguration) obj;
		return java.util.Objects.equals(columns, other.columns)
				&& java.util.Objects.equals(separators, other.separators);
	}

	

}
