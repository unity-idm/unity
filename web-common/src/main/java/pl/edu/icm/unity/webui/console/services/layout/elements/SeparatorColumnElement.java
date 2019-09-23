/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.layout.elements;

import java.util.function.Consumer;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.binding.I18nStringBindingValue;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.console.services.authnlayout.ColumnElement;
import pl.edu.icm.unity.webui.console.services.authnlayout.ColumnElementBase;
import pl.edu.icm.unity.webui.console.services.authnlayout.ColumnElementWithValue;

/**
 * 
 * @author P.Piernik
 *
 */
public class SeparatorColumnElement extends ColumnElementBase implements ColumnElementWithValue<I18nString>
{
	private I18nTextField valueField;
	private Binder<I18nStringBindingValue> binder;

	public SeparatorColumnElement(UnityMessageSource msg, Consumer<ColumnElement> removeElementListener,
			Runnable valueChangeListener, Runnable dragStart, Runnable dragStop)
	{
		super(msg, msg.getMessage("AuthnColumnLayoutElement.separator"), Images.text, dragStart,
				dragStop, removeElementListener);
		addContent(getContent());
		addValueChangeListener(valueChangeListener);
	}

	@Override
	public void setValue(I18nString v)
	{
		binder.setBean(new I18nStringBindingValue(v));
	}

	@Override
	public I18nString getValue()
	{
		return binder.getBean().getValue();
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
		try
		{
			validate();
		} catch (FormValidationException e)
		{
			// ok
		}
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