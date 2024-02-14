/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.utils.tprofile;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.console.tprofile.ActionParameterComponentProvider;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;


class TranslationProfileFieldFactoryBase
{
	private final MessageSource msg;
	private final ProfileType type;
	private final TypesRegistryBase<? extends TranslationActionFactory<?>> registry;
	private final ActionParameterComponentProvider actionComponentProvider;
	private final HtmlTooltipFactory htmlTooltipFactory;
	private final NotificationPresenter notificationPresenter;
	private final String caption;

	TranslationProfileFieldFactoryBase(String caption, MessageSource msg, ProfileType type,
			TypesRegistryBase<? extends TranslationActionFactory<?>> registry,
			ActionParameterComponentProvider actionComponentProvider, NotificationPresenter notificationPresenter,
			HtmlTooltipFactory htmlTooltipFactory)
	{

		this.caption = caption;
		this.type = type;
		this.msg = msg;
		this.registry = registry;
		this.actionComponentProvider = actionComponentProvider;
		this.htmlTooltipFactory = htmlTooltipFactory;
		this.notificationPresenter = notificationPresenter;
	}

	public TranslationProfileField getInstance(SubViewSwitcher subViewSwitcher)
	{
		return new TranslationProfileField(msg, type, registry, actionComponentProvider, subViewSwitcher,
				notificationPresenter, htmlTooltipFactory);
	}

	public AccordionPanel getWrappedFieldInstance(SubViewSwitcher subViewSwitcher, Binder<?> binder,
			String fieldName)
	{
		TranslationProfileField field = getInstance(subViewSwitcher);
		binder.forField(field).bind(fieldName);
		AccordionPanel accordionPanel = new AccordionPanel(caption, field);
		accordionPanel.setWidthFull();
		return accordionPanel;
	}
}
