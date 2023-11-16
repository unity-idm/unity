/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.apache.commons.validator.routines.UrlValidator;

public class ImageField extends CustomField<String>
{
	private final TextField field;

	public ImageField()
	{
		UrlValidator validator = new UrlValidator();
		Image image = new Image();
		image.addClassName("u-img-field");
		image.setVisible(false);
		Icon clearButton = new Icon("lumo", "cross");
		clearButton.setVisible(false);
		field = new TextField();

		field.addValueChangeListener(event ->
		{
			if(validator.isValid(event.getValue()))
			{
				image.setSrc(event.getValue());
				image.setVisible(true);
				clearButton.setVisible(true);
				field.getElement().removeAttribute("invalid");
				field.getElement().removeAttribute("has-error-message");
			}
			else
			{
				image.setVisible(false);
				clearButton.setVisible(false);
				field.getElement().setAttribute("invalid", true);
				field.getElement().setAttribute("has-error-message", true);
			}
		});

		clearButton.addClickListener(e ->
		{
			image.setVisible(false);
			clearButton.setVisible(false);
			field.setValue("");
		});

		VerticalLayout layout = new VerticalLayout();
		layout.setPadding(false);
		layout.add(field, clearButton, image);
		add(layout);
	}

	@Override
	public void setWidth(String width)
	{
		field.setWidth(width);
	}

	@Override
	protected String generateModelValue()
	{
		return field.getValue();
	}

	@Override
	protected void setPresentationValue(String s)
	{
		field.setValue(s);
	}
}
