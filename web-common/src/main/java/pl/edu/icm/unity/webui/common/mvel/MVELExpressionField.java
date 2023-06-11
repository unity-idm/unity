/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.mvel;

import com.vaadin.data.Binder;
import com.vaadin.data.ValueProvider;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.Setter;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Plain text field allowing for editing an MVEL expression
 * 
 * @author K. Benedyczak
 */
public class MVELExpressionField extends CustomField<String>
{
	private MVELExpressionEditor editor;
	private TextField field;
	private Button editorButton;
	private HorizontalLayout layout;
	private String value;
	private boolean mandatory;
	private MVELExpressionContext context;

	public MVELExpressionField(MessageSource msg, String caption, String description, MVELExpressionContext context)
	{
		this.field = new TextField();
		this.editorButton = new Button(Images.cogs.getResource());
		this.context = context;

		editorButton.addStyleName(Styles.toolbarButton.toString());
		editorButton.addStyleName(Styles.vButtonLink.toString());
		editorButton.addClickListener(e -> new MVELExpressionEditorDialog(msg, this.context, mandatory,
				field.getValue(), v -> field.setValue(v)).show());
		this.editor = new MVELExpressionEditor(this, msg);

		layout = new HorizontalLayout();
		layout.setSpacing(false);
		layout.addComponent(field);
		layout.addComponent(editorButton);
		layout.setExpandRatio(field, 1);
		layout.setExpandRatio(editorButton, 0);
		layout.setComponentAlignment(editorButton, Alignment.MIDDLE_RIGHT);
		setCaption(caption);
		setDescription(description, ContentMode.HTML);
		field.addValueChangeListener(e ->
		{
			value = e.getValue();
			fireEvent(new ValueChangeEvent<>(this, getValue(), e.isUserOriginated()));
		});

	}

	public void configureBinding(Binder<?> binder, String fieldName, boolean mandatory)
	{
		this.mandatory = mandatory;
		editor.configureBinding(binder, fieldName, mandatory);
	}

	public void configureBinding(Binder<String> binder, ValueProvider<String, String> getter,
			Setter<String, String> setter, boolean mandatory)
	{
		this.mandatory = mandatory;
		editor.configureBinding(binder, getter, setter, mandatory);
	}

	@Override
	public String getValue()
	{
		return value;
	}

	@Override
	public String getEmptyValue()
	{
		return "";
	}

	@Override
	protected Component initContent()
	{
		return layout;
	}

	@Override
	protected void doSetValue(String value)
	{
		if (value != null)
			field.setValue(value);
		this.value = value;
	}

	protected void addBlurListener(BlurListener listener)
	{
		field.addBlurListener(listener);
	}

	@Override
	public void focus()
	{
		field.focus();
	}

	@Override
	public void setComponentError(ErrorMessage componentError)
	{
		super.setComponentError(componentError);
		if (componentError != null)
			field.addStyleName("error");
		else
			field.removeStyleName("error");
	}

	@Override
	public void setStyleName(String style)
	{
		field.addStyleName(style);
		editorButton.addStyleName(style);
		layout.addStyleName(style);
	}

	@Override
	public void setReadOnly(boolean readOnly)
	{
		field.setReadOnly(readOnly);
		editorButton.setEnabled(!readOnly);
	}

	public void setContext(MVELExpressionContext context)
	{
		this.context = context;
	}
	
	public MVELExpressionContext getContext()
	{
		return context;
	}
	
	@Override
	public void setWidth(float width, Unit unit)
	{
		if (field != null)
		{
			field.setWidth(width, unit);
		}
	}
}
