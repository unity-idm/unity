/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.layout.configuration;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;

import io.imunity.vaadin.auth.services.layout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;

/**
 * Stores information about authentication layout column configuration
 * 
 * @author P.Piernik
 *
 */
public class AuthnLayoutColumnConfiguration
{
	public final I18nString title;
	public final int width;
	public final List<AuthnElementConfiguration> contents;

	public AuthnLayoutColumnConfiguration(I18nString title, int width, List<AuthnElementConfiguration> contents)
	{
		this.title = title;
		this.width = width;
		this.contents = Collections.unmodifiableList(contents);
	}
	

	@Override
	public int hashCode()
	{
		return Objects.hashCode(title, width, contents);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final AuthnLayoutColumnConfiguration other = (AuthnLayoutColumnConfiguration) obj;
	
		return Objects.equal(this.title, other.title) && Objects.equal(this.width, other.width)
				&& Objects.equal(this.contents, other.contents);
	}
}
