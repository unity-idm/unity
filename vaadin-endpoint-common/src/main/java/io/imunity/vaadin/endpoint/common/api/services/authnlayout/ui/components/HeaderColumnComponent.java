/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components;

import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.HeaderConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.ColumnComponent;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.ColumnComponentBase;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.binding.I18nStringBindingValue;


public class HeaderColumnComponent extends ColumnComponentBase
{
	private LocalizedTextFieldDetails valueField;
	private Binder<I18nStringBindingValue> binder;

	public HeaderColumnComponent(MessageSource msg, Consumer<ColumnComponent> removeElementListener,
			Runnable valueChangeListener, Runnable dragStart, Runnable dragStop)
	{
		super(msg, msg.getMessage("AuthnColumnLayoutElement.header"), VaadinIcon.HEADER, dragStart,
				dragStop, removeElementListener);
		addContent(getContent());
		addValueChangeListener(valueChangeListener);
	}

	@Override
	public void setConfigState(AuthnElementConfiguration v)
	{
		binder.setBean(new I18nStringBindingValue(((HeaderConfig) v).headerText));
	}

	@Override
	public AuthnElementConfiguration getConfigState()
	{
		return new HeaderConfig(binder.getBean().getValue());
	}

	private Component getContent()
	{
		binder = new Binder<>(I18nStringBindingValue.class);
		valueField = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		binder.forField(valueField).withConverter(I18nString::new, I18nString::getLocalizedMap).bind("value");
		binder.setBean(new I18nStringBindingValue(new I18nString()));
		return valueField;
	}

	@Override
	public void refresh()
	{
		binder.validate();
	}

	@Override
	public void validate() throws FormValidationException
	{
		if (binder.validate().hasErrors())
		{
			throw new FormValidationException();
		}
	}

	@Override
	public void addValueChangeListener(Runnable valueChange)
	{
		valueField.addValueChangeListener(e -> valueChange.run());	
	}

}