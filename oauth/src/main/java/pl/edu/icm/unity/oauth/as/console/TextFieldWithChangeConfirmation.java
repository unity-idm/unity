/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.Binder;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.binding.StringBindingValue;

/**
 * 
 * @author P.Piernik
 *
 */
public class TextFieldWithChangeConfirmation<T extends CustomField<String>> extends CustomField<String>
{
	private final MessageSource msg;
	private final T field;
	private final Binder<StringBindingValue> binder;
	private boolean editMode;
	private String realValue;

	public TextFieldWithChangeConfirmation(MessageSource msg, T content)
	{
		this.msg = msg;
		binder = new Binder<>(StringBindingValue.class);
		field = content;
		add(initContent());
	}

	@Override
	public String getValue()
	{
		if (!editMode)
		{
			return realValue;
		} else
		{
			return null;
		}
	}

	private Component initContent()
	{
		HorizontalLayout mainLayout = new HorizontalLayout();

		Button ok = new Button();
		ok.setText(msg.getMessage("ok"));
		ok.setVisible(false);
		Button cancel = new Button();
		cancel.setText(msg.getMessage("cancel"));
		cancel.setVisible(false);

		Button change = new Button();
		change.setText(msg.getMessage("update"));
		change.addClickListener(e -> {
			field.setVisible(true);
			change.setVisible(false);
			ok.setVisible(true);
			cancel.setVisible(true);
			editMode = true;
		});

		ok.addClickListener(e -> {

			if (binder.validate().hasErrors())
			{
				return;
			}

			editMode = false;
			realValue = field.getValue();
			fireEvent(new ComponentValueChangeEvent<>(this, this, field.getValue(), true));
			field.setVisible(false);
			change.setVisible(true);
			ok.setVisible(false);
			cancel.setVisible(false);

		});

		cancel.addClickListener(e -> {
			field.setVisible(false);
			change.setVisible(true);
			ok.setVisible(false);
			cancel.setVisible(false);
			editMode = false;
		});

		field.setVisible(false);
		binder.forField(field).asRequired().bind("value");
		field.setRequiredIndicatorVisible(false);
		
		mainLayout.add(change);
		mainLayout.add(field);
		mainLayout.add(ok);
		mainLayout.add(cancel);

		return mainLayout;

	}

	@Override
	public void setValue(String value)
	{
		realValue = value;
	}

	@Override
	public void setWidth(float width, Unit unit)
	{
		if (field != null)
		{
			field.setWidth(width, unit);
		}
	}
	
	public boolean isEditMode()
	{
		return editMode;
	}

	@Override
	protected String generateModelValue()
	{
		return field.getValue();
	}

	@Override
	protected void setPresentationValue(String s)
	{
		setValue(s);
	}
}
