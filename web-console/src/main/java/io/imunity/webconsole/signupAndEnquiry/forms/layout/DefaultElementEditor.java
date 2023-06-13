/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.forms.layout;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.registration.layout.FormElement;

/**
 * Editor of {@link FormElement}. Simply presents what was set.
 * @author K. Benedyczak
 */
class DefaultElementEditor extends CustomComponent implements FormElementEditor<FormElement>
{
	private FormElement element;
	private Label label;
	
	DefaultElementEditor(FormElement element)
	{
		initUI();
		this.element = element;
		label.setValue(this.element.toString());
	}

	@Override
	public FormElement getElement()
	{
		return element;
	}

	private void initUI()
	{
		label = new Label();
		setCompositionRoot(label);
	}
}
