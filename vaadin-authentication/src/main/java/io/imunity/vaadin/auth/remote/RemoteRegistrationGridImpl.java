/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.remote;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin.auth.AuthnsGridWidget;
import io.imunity.vaadin.auth.SearchComponent;
import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationGrid;
import pl.edu.icm.unity.base.message.MessageSource;


record RemoteRegistrationGridImpl(MessageSource msg, AuthnsGridWidget authnsGridWidget, boolean empty) implements RemoteRegistrationGrid
{

	@Override
	public Component getComponent()
	{
		return authnsGridWidget;
	}


	@Override
	public Component getSearchComponent()
	{
		return new SearchComponent(msg, authnsGridWidget::filter);
	}

	@Override
	public boolean isEmpty()
	{
		return empty;
	}
}
