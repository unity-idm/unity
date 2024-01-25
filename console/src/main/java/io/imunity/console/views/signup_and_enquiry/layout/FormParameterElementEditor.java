/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.layout;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.base.registration.layout.FormParameterElement;

/**
 * Editor of {@link FormParameterElement}. Simply presents what was set but with additional information
 * on actual name of the parameter that is on the given position.
 * @author K. Benedyczak
 */
class FormParameterElementEditor extends VerticalLayout implements FormElementEditor<FormParameterElement>
{
	private final FormParameterElement element;
	private Span label;
	
	FormParameterElementEditor(FormParameterElement element)
	{
		initUI();
		this.element = element;
		label.setText(this.element.toString());
	}
	
	@Override
	public FormParameterElement getComponent()
	{
		return element;
	}

	private void initUI()
	{
		setPadding(false);
		label = new Span();
		add(label);
	}
}
