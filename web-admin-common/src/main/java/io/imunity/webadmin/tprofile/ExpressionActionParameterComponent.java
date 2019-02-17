/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.tprofile;

import java.util.Optional;

import com.vaadin.data.Binder;
import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.ui.dnd.DropTargetExtension;

import io.imunity.webadmin.tprofile.wizard.DragDropBean;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.webui.common.mvel.MVELExpressionField;

/**
 * For editing MVEL expressions. Decorates the {@link MVELExpressionField} with
 * drag'n'drop support.
 * 
 * @author Roman Krysinski
 * @author Piotr Piernik
 *
 */
public class ExpressionActionParameterComponent extends MVELExpressionField
		implements ActionParameterComponent
{
	private Binder<StringValueBean> binder;

	public ExpressionActionParameterComponent(ActionParameterDefinition param,
			UnityMessageSource msg)
	{
		super(msg, param.getName() + ":", msg.getMessage(param.getDescriptionKey()));
		binder = new Binder<>(StringValueBean.class);
		configureBinding(binder, "value", param.isMandatory());
		binder.setBean(new StringValueBean());
		addBlurListener(event -> markAsDirtyRecursive());
		addDropHandler();
		setWidth(100, Unit.PERCENTAGE);
	}

	private void addDropHandler()
	{
		DropTargetExtension<ExpressionActionParameterComponent> dropTarget = new DropTargetExtension<>(
				this);
		dropTarget.setDropEffect(DropEffect.MOVE);

		dropTarget.addDropListener(event -> {
			Optional<?> dragData = event.getDragData();
			if (dragData.isPresent() && dragData.get() instanceof DragDropBean)
			{
				DragDropBean bean = (DragDropBean) dragData.get();
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
		binder.addValueChangeListener((e) -> {
			callback.run();
		});
	}
}