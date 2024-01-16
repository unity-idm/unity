/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.layout;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.base.registration.layout.FormElement;

/**
 * Editor of {@link FormElement}. Simply presents what was set.
 * @author K. Benedyczak
 */
class DefaultElementEditor extends VerticalLayout implements FormElementEditor<FormElement>
{
	private final FormElement element;
	private Span label;
	
	DefaultElementEditor(FormElement element)
	{
		setPadding(false);
		initUI();
		this.element = element;
		label.setText(this.element.toString());
	}

	@Override
	public FormElement getComponent()
	{
		return element;
	}

	private void initUI()
	{
		label = new Span();
		add(label);
	}
}
