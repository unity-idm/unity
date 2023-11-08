/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_browser;

import com.vaadin.flow.component.combobox.ComboBox;

public class ComboBoxNonEmptyValueSupport
{
	public static <T> void install(ComboBox<T> comboBox)
	{
		comboBox.addValueChangeListener(e ->
		{
			if(e.getValue() == null)
				comboBox.setValue(e.getOldValue());
		});
	}
}
