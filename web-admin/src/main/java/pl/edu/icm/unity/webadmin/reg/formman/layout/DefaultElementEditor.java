/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman.layout;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.types.registration.layout.FormElement;

/**
 * Editor of {@link FormElement}. Simply presents what was set.
 * @author K. Benedyczak
 */
public class DefaultElementEditor extends CustomComponent implements FormElementEditor<FormElement>
{
	private FormElement element;
	private Label label;
	
	public DefaultElementEditor(FormElement element)
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
