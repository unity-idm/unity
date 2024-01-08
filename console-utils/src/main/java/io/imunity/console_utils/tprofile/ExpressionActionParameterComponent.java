/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console_utils.tprofile;

import com.vaadin.flow.data.binder.Binder;

import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.mvel.MVELExpressionField;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;

/**
 * For editing MVEL expressions. Decorates the {@link MVELExpressionField} with
 * drag'n'drop support.
 */
public class ExpressionActionParameterComponent extends MVELExpressionField implements ActionParameterComponent
{
	private final Binder<StringValueBean> binder;
	private String label;

	public ExpressionActionParameterComponent(ActionParameterDefinition param, MessageSource msg, HtmlTooltipFactory htmlTooltipFactory)
	{
		super(msg, param.getName() + ":", msg.getMessage(param.getDescriptionKey()),
				(MVELExpressionContext) param.getDetails()
						.get(), htmlTooltipFactory);
		binder = new Binder<>(StringValueBean.class);
		configureBinding(binder, "value", param.isMandatory());
		binder.setBean(new StringValueBean());	
		setWidthFull();
	}

	@Override
	public String getActionValue()
	{
		return binder.getBean()
				.getValue();
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
	
	@Override
	public void setLabel(String label)
	{
		this.label = label;
		super.setLabel(label);
	}
	
	@Override
	public String getLabel()
	{
		return label;
	}
	
	public ExpressionActionParameterComponent applyContext(EditorContext editorContext)
	{
		if (editorContext.equals(EditorContext.WIZARD))
		{
			addClassNameToField(CssClassNames.WIDTH_FULL.getName());
		}
		return this;
	}

}