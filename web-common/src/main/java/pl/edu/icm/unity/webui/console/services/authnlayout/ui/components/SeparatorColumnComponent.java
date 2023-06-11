/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.ui.components;

import java.util.function.Consumer;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.binding.I18nStringBindingValue;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.SeparatorConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponent;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponentBase;

/**
 * 
 * @author P.Piernik
 *
 */
public class SeparatorColumnComponent extends ColumnComponentBase
{
	private I18nTextField valueField;
	private Binder<I18nStringBindingValue> binder;

	public SeparatorColumnComponent(MessageSource msg, Consumer<ColumnComponent> removeElementListener,
			Runnable valueChangeListener, Runnable dragStart, Runnable dragStop)
	{
		super(msg, msg.getMessage("AuthnColumnLayoutElement.separator"), Images.text, dragStart,
				dragStop, removeElementListener);
		addContent(getContent());
		addValueChangeListener(valueChangeListener);
	}

	@Override
	public void setConfigState(AuthnElementConfiguration v)
	{
		binder.setBean(new I18nStringBindingValue(((SeparatorConfig)v).separatorText));
	}

	@Override
	public AuthnElementConfiguration getConfigState()
	{
		return new SeparatorConfig(binder.getBean().getValue());
	}

	private Component getContent()
	{
		binder = new Binder<>(I18nStringBindingValue.class);
		valueField = new I18nTextField(msg);
		valueField.setWidth(20, Unit.EM);
		binder.forField(valueField).bind("value");
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