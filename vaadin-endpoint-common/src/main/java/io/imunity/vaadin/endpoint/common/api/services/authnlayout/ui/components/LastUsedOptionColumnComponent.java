/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components;

import java.util.function.Consumer;

import com.vaadin.flow.component.icon.VaadinIcon;

import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.LastUsedConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.ColumnComponent;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.ColumnComponentBase;
import pl.edu.icm.unity.base.message.MessageSource;


/**
 * 
 * @author P.Piernik
 *
 */
public class LastUsedOptionColumnComponent extends ColumnComponentBase
{

	public LastUsedOptionColumnComponent(MessageSource msg, Consumer<ColumnComponent> removeElementListener,
			Runnable dragStart, Runnable dragStop)
	{
		super(msg, msg.getMessage("AuthnColumnLayoutElement.lastUsedOption"), VaadinIcon.STAR,
				dragStart, dragStop, removeElementListener);
	}

	@Override
	public void setConfigState(AuthnElementConfiguration state)
	{
		
	}

	@Override
	public LastUsedConfig getConfigState()
	{
		return new LastUsedConfig();
	}

}