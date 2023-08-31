/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_setup.automation.mvel;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.function.ValueProvider;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;

/**
 * Plain text field allowing for editing an MVEL expression
 */
public class MVELExpressionField extends CustomField<String>
{
	private final MVELExpressionEditor editor;
	private final TextField field;
	private final Icon editorButton;
	private String value;
	private boolean mandatory;
	private MVELExpressionContext context;

	public MVELExpressionField(MessageSource msg, String caption, String description, MVELExpressionContext context)
	{
		this.field = new TextField();
		this.editorButton = VaadinIcon.COGS.create();
		this.context = context;

		editorButton.addClickListener(e -> new MVELExpressionEditorDialog(msg, this.context, mandatory,
				field.getValue(), field::setValue).open());
		editorButton.getStyle().set("cursor", "pointer");
		this.editor = new MVELExpressionEditor(this, msg);

		HorizontalLayout layout = new HorizontalLayout();
		layout.setSpacing(false);
		VerticalLayout iconLayout = new VerticalLayout(editorButton);
		iconLayout.getStyle().set("padding-left", "0.2em");
		layout.add(field, iconLayout);
		field.setLabel(caption);
		field.setTooltipText(description);
		editorButton.setTooltipText(description);
		field.addValueChangeListener(e ->
		{
			value = e.getValue();
			fireEvent(new ComponentValueChangeEvent<>(this, e.getSource(), getValue(), e.isFromClient()));
		});
		add(layout);
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
	public void focus()
	{
		field.focus();
	}

	@Override
	public void setReadOnly(boolean readOnly)
	{
		field.setReadOnly(readOnly);
		editorButton.getStyle().set("opacity", readOnly ? "0.5" : "1");
	}

	public void setContext(MVELExpressionContext context)
	{
		this.context = context;
	}
	
	public MVELExpressionContext getContext()
	{
		return context;
	}
	
	public void setWidth(float width, Unit unit)
	{
		if (field != null)
		{
			field.setWidth(width, unit);
		}
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
