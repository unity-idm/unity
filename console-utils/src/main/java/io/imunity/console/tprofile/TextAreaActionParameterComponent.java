/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.elements.VaadinElementReadOnlySetter;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;


public class TextAreaActionParameterComponent extends TextArea implements ActionParameterComponent
{
	private final Binder<StringValueBean> binder;
	private String caption;

	public TextAreaActionParameterComponent(ActionParameterDefinition desc, MessageSource msg)
	{
		super(desc.getName() + ":");
		setTooltipText(msg.getMessage(desc.getDescriptionKey()));
		setWidth(TEXT_FIELD_BIG.value());
		binder = new Binder<>(StringValueBean.class);
		if (desc.isMandatory())
		{
			binder.forField(this).asRequired(msg.getMessage("fieldRequired")).bind("value");

		} else
		{
			binder.forField(this).bind("value");
		}
		binder.setBean(new StringValueBean());		
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
	public void addValueChangeCallback(Runnable callback)
	{
		binder.addValueChangeListener((e) -> { callback.run(); });		
	}

	@Override
	public boolean isValid()
	{
		binder.validate();
		return binder.isValid();
	}

	@Override
	public void setReadOnly(boolean readOnly)
	{
		VaadinElementReadOnlySetter.setReadOnly(getElement(), readOnly);
	}

	@Override
	public void setLabel(String label)
	{
		if(caption == null)
			caption = label;
		super.setLabel(label);
	}
	
	@Override
	public String getCaption()
	{
		return caption;
	}
}
