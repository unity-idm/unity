/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.binding;

import com.google.common.base.Objects;
import com.vaadin.data.Binder;

import pl.edu.icm.unity.base.i18n.I18nString;

/**
 * Useful for trivial {@link Binder}s that should operate on a single field of
 * I18nString type
 * 
 * @author P.Piernik
 */
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