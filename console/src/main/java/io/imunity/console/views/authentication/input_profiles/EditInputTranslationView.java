/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.input_profiles;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;

@PermitAll
@Route(value = "/remote-data-profile/edit", layout = ConsoleMenu.class)
class EditInputTranslationView extends EditTranslationView
{
	public static final String VIEW_NAME = "EditInputTranslation";

	@Autowired
	EditInputTranslationView(MessageSource msg, InputTranslationsService controller, NotificationPresenter notificationPresenter)
	{
		super(msg, controller, notificationPresenter);
	}

	@Override
	public Class<? extends ConsoleViewComponent> getViewAll()
	{
		return RemoteDataProfilesView.class;
	}

	

	

	

}