/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.identity_provider.released_profile;

import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.translation_profiles.TranslationsView;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.identityProvider.releasedProfiles", parent = "WebConsoleMenu.identityProvider")
@Route(value = "/released-data-profiles", layout = ConsoleMenu.class)
public class ReleasedDataProfilesView extends TranslationsView
{

	public ReleasedDataProfilesView(MessageSource msg, OutputTranslationsService controller,
			NotificationPresenter notificationPresenter)
	{
		super(msg, controller, notificationPresenter);
	}

	@Override
	protected Class<? extends ConsoleViewComponent> getEditView()
	{

		return EditOutputTranslationView.class;
	}

	@Override
	protected Class<? extends ConsoleViewComponent> getNewView()
	{

		return NewOutputTranslationView.class;
	}

	@Override
	public String getHeaderCaption()
	{
		return msg.getMessage("OutputTranslationsView.headerCaption");
	}

	@Override
	protected String getConfirmDeleteText(String profiles)
	{
		return msg.getMessage("OutputTranslationsView.confirmDelete", profiles);
	}

}
