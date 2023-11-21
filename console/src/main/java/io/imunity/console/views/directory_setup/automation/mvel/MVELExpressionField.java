/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_setup.automation.mvel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.function.ValueProvider;
import io.imunity.console.components.TooltipFactory;
import io.imunity.console.tprofile.DragDropBean;
import io.imunity.vaadin.elements.FormItemRequiredIndicatorHandler;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;

import java.util.Collection;
import java.util.Optional;

import static io.imunity.vaadin.elements.CSSVars.BASE_MARGIN;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.FIELD_ICON_GAP;
import static io.imunity.vaadin.elements.CssClassNames.POINTER;

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

	public MVELExpressionField(MessageSource msg, String description, MVELExpressionContext context)
	{
		this.field = new TextField();
		field.setWidth(TEXT_FIELD_BIG.value());
		this.editorButton = VaadinIcon.COGS.create();
		this.context = context;

		editorButton.addClickListener(e -> new MVELExpressionEditorDialog(msg, this.context, mandatory,
				field.getValue(), field::setValue).open());
		editorButton.setClassName(POINTER.getName());
		this.editor = new MVELExpressionEditor(this, msg);

		HorizontalLayout layout = new HorizontalLayout();
		layout.setSpacing(false);
		Component tooltip = TooltipFactory.getWithHtmlContent(description);
		HorizontalLayout iconsLayout = new HorizontalLayout(editorButton, tooltip);
		iconsLayout.setSpacing(false);
		iconsLayout.getStyle().set("margin-top", BASE_MARGIN.value());
		iconsLayout.setClassName(FIELD_ICON_GAP.getName());
		layout.add(field, iconsLayout);

		field.addValueChangeListener(e ->
		{
			value = e.getValue();
			fireEvent(new ComponentValueChangeEvent<>(this, e.getSource(), getValue(), e.isFromClient()));
		});
		add(layout);
		addDropHandler();
	}
	
	private void addDropHandler()
	{
		DropTarget<MVELExpressionField> dropTarget = DropTarget.create(this);
		dropTarget.setDropEffect(DropEffect.MOVE);

		dropTarget.addDropListener(event ->
		{
			Optional<?> dragData = event.getDragData();
			if (dragData.isPresent() && dragData.get() instanceof Collection)
			{
				Object next = ((Collection<?>) dragData.get()).iterator().next();
				if (next instanceof DragDropBean dragDropBean)
					field.setValue((field.getValue() != null ? field.getValue() : "") + dragDropBean.getExpression());
			}
		});
	}
	
	@Override
	public void addClassName(String className)
	{
		field.addClassName(className);
	}
	
	@Override
	public boolean removeClassName(String className)
	{
		return field.removeClassName(className);
	}
	
	public MVELExpressionField(MessageSource msg, String caption, String description, MVELExpressionContext context)
	{
		this(msg, description, context);
		setLabel(caption);
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
		return field.getValue();
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
		return value;
	}

	@Override
	protected void setPresentationValue(String s)
	{
		field.setValue(s);
	}

	@Override
	public void setInvalid(boolean invalid)
	{
		super.setInvalid(invalid);
		field.setInvalid(invalid);
		FormItemRequiredIndicatorHandler.setInvalid(this, invalid);
	}
}
