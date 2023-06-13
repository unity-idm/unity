/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.forms.layout;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.registration.layout.FormParameterElement;

/**
 * Editor of {@link FormParameterElement}. Simply presents what was set but with additional information
 * on actual name of the parameter that is on the given position.
 * @author K. Benedyczak
 */
class FormParameterElementEditor extends CustomComponent implements FormElementEditor<FormParameterElement>
{
	private FormParameterElement element;
	private Label label;
	
	FormParameterElementEditor(FormParameterElement element)
	{
		initUI();
		this.element = element;
		label.setValue(this.element.toString());
	}
	
	@Override
	public FormParameterElement getElement()
	{
		return element;
	}

	private void initUI()
	{
		label = new Label();
		setCompositionRoot(label);
	}
}
