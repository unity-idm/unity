/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.layout.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.auth.services.layout.configuration.elements.AuthnElementConfiguration;
import io.imunity.vaadin.elements.CSSVars;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.auth.services.layout.configuration.elements.SeparatorConfig;
import io.imunity.vaadin.auth.services.layout.ui.ColumnComponent;
import io.imunity.vaadin.auth.services.layout.ui.ColumnComponentBase;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.function.Consumer;



/**
 * 
 * @author P.Piernik
 *
 */
public class SeparatorColumnComponent extends ColumnComponentBase
{
	private LocalizedTextFieldDetails valueField;
	private Binder<I18nStringBindingValue> binder;

	public SeparatorColumnComponent(MessageSource msg, Consumer<ColumnComponent> removeElementListener,
			Runnable valueChangeListener, Runnable dragStart, Runnable dragStop)
	{
		super(msg, msg.getMessage("AuthnColumnLayoutElement.separator"), VaadinIcon.TEXT_LABEL, dragStart,
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
		valueField = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		binder.forField(valueField).withConverter(I18nString::new, I18nString::getLocalizedMap).bind("value");
		binder.setBean(new I18nStringBindingValue(new I18nString()));
		valueField.setWidthFull();
		valueField.getElement().getStyle().set("width", "100%");	
		VerticalLayout main = new VerticalLayout();
		main.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());
		main.setMargin(false);
		main.setPadding(false);
		main.setSpacing(false);
		main.add(valueField);
		return main;
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