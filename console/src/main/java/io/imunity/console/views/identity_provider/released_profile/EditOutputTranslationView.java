/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.identity_provider.released_profile;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.translation_profiles.EditTranslationView;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;

@PermitAll
@Route(value = "/released-data-profile/edit", layout = ConsoleMenu.class)
public class EditOutputTranslationView extends EditTranslationView
{
	@Autowired
	EditOutputTranslationView(MessageSource msg, OutputTranslationsService controller, NotificationPresenter notificationPresenter)
	{
		super(msg, controller, notificationPresenter);
	}

	@Override
	public Class<? extends ConsoleViewComponent> getViewAll()
	{
		return ReleasedDataProfilesView.class;
	}

	

	

	

}
