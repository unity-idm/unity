/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dependency.CssImport;

import java.util.Set;

@CssImport(value = "./styles/components/check-box-with-error.css", themeFor = "vaadin-checkbox-group")
public class CheckboxWithError extends CheckboxGroup<String>
{
	private final String label;
	public CheckboxWithError(String label)
	{
		this.label = label;
		setItems(Set.of(label));
		setItemLabelGenerator(x -> "");
		addValueChangeListener(e -> setInvalid(false));
		add(new Html("<div>" + label + "</div>"));
		setClassName("check-box-with-error");
	}

	public boolean getState()
	{
		return getValue().contains(label);
	}

	public void setValue(boolean value)
	{
		if(value)
			setValue(Set.of(label));
		else
			deselect(label);
	}
}
