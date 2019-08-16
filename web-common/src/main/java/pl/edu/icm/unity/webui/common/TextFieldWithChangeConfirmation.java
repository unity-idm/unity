/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import com.vaadin.data.Binder;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.binding.StringBindingValue;

/**
 * 
 * @author P.Piernik
 *
 */
public class TextFieldWithChangeConfirmation extends CustomField<String>
{
	private UnityMessageSource msg;
	private TextField field;
	private boolean editMode;
	private Binder<StringBindingValue> binder;

	public TextFieldWithChangeConfirmation(UnityMessageSource msg)
	{
		this.msg = msg;
		binder = new Binder<>(StringBindingValue.class);
		field = new TextField();
	}

	@Override
	public String getValue()
	{
		if (!editMode)
		{
			return field.getValue();
		} else
		{
			return null;
		}
	}

	@Override
	protected Component initContent()
	{
		HorizontalLayout mainLayout = new HorizontalLayout();

		Button ok = new Button();
		ok.setCaption(msg.getMessage("ok"));
		ok.setVisible(false);
		Button cancel = new Button();
		cancel.setCaption(msg.getMessage("cancel"));
		cancel.setVisible(false);

		Button change = new Button();
		change.setCaption(msg.getMessage("update"));
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
			fireEvent(new ValueChangeEvent<String>(this, field.getValue(), true));
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
		
		mainLayout.addComponent(change);
		mainLayout.addComponent(field);
		mainLayout.addComponent(ok);
		mainLayout.addComponent(cancel);

		return mainLayout;

	}

	@Override
	protected void doSetValue(String value)
	{

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
}
