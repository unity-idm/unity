/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.services.layout.ui.components;

import com.google.common.base.Objects;
import pl.edu.icm.unity.base.i18n.I18nString;


public class I18nStringBindingValue
{
	private I18nString value;

	public I18nStringBindingValue(I18nString value)
	{
		this.value = value;
	}

	public I18nString getValue()
	{
		return value;
	}

	public void setValue(I18nString value)
	{
		this.value = value;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hashCode(value);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final I18nStringBindingValue other = (I18nStringBindingValue) obj;

		return Objects.equal(this.value, other.value);
	}
}