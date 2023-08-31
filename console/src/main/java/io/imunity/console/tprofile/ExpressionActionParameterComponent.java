/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.console.views.directory_setup.automation.mvel.MVELExpressionField;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;

import java.util.Optional;

/**
 * For editing MVEL expressions. Decorates the {@link MVELExpressionField} with
 * drag'n'drop support.
 */
public class ExpressionActionParameterComponent extends MVELExpressionField implements ActionParameterComponent
{
	private final Binder<StringValueBean> binder;

	public ExpressionActionParameterComponent(ActionParameterDefinition param, MessageSource msg)
	{
		super(msg, param.getName() + ":", msg.getMessage(param.getDescriptionKey()),
				(MVELExpressionContext) param.getDetails().get());
		binder = new Binder<>(StringValueBean.class);
		configureBinding(binder, "value", param.isMandatory());
		binder.setBean(new StringValueBean());
		addDropHandler();
		setWidthFull();
	}

	private void addDropHandler()
	{
		DropTarget<ExpressionActionParameterComponent> dropTarget = DropTarget.create(this);
		dropTarget.setDropEffect(DropEffect.MOVE);

		dropTarget.addDropListener(event -> {
			Optional<?> dragData = event.getDragData();
			if (dragData.isPresent() && dragData.get() instanceof DragDropBean bean)
			{
				setValue(getValue() + bean.getExpression());
			}
		});
	}

	@Override
	public String getActionValue()
	{
		return binder.getBean().getValue();
	}

	@Override
	public void setActionValue(String value)
	{
		binder.setBean(new StringValueBean(value));

	}

	@Override
	public boolean isValid()
	{
		binder.validate();
		return binder.isValid();
	}

	@Override
	public void addValueChangeCallback(Runnable callback)
	{
		binder.addValueChangeListener((e) -> callback.run());
	}
}