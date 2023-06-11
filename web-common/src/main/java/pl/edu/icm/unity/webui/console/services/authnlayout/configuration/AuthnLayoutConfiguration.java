/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;

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
		return Objects.hashCode(columns, separators);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final AuthnLayoutConfiguration other = (AuthnLayoutConfiguration) obj;

		return Objects.equal(this.columns, other.columns) && Objects.equal(this.separators, other.separators);

	}
}
