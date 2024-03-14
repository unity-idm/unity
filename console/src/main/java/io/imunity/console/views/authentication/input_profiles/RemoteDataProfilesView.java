/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.input_profiles;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.ShowViewActionLayoutFactory;
import io.imunity.console.views.translation_profiles.TranslationsView;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;

/**
 * Lists all input translation profiles
 * 
 * @author P.Piernik
 *
 */
@PermitAll
@Breadcrumb(key = "WebConsoleMenu.authentication.inputTranslation", parent = "WebConsoleMenu.authentication")
@Route(value = "/remote-data-profiles", layout = ConsoleMenu.class)
public class RemoteDataProfilesView extends TranslationsView
{
	@Autowired
	public RemoteDataProfilesView(MessageSource msg, InputTranslationsService controller,
			NotificationPresenter notificationPresenter)
	{
		super(msg, controller, notificationPresenter);
	}

	@Override
	protected Class<? extends ConsoleViewComponent> getEditView()
	{
		return EditInputTranslationView.class;
	}

	@Override
	protected Class<? extends ConsoleViewComponent> getNewView()
	{
		return NewInputTranslationView.class;
	}

	@Override
	public String getHeaderCaption()
	{
		return msg.getMessage("InputTranslationsView.headerCaption");
	}

	protected List<Button> getButtonsBar()
	{
		Button wizard = ShowViewActionLayoutFactory.buildActionButton(msg.getMessage("InputTranslationsView.wizard"),
				VaadinIcon.MAGIC, e -> showWizardDialog());
		return Stream.concat(Stream.of(wizard), super.getButtonsBar().stream())
				.collect(Collectors.toList());
	}

	private void showWizardDialog()
	{
		try
		{
			 ((InputTranslationsService) controller).getWizardDialog(() -> refreshProfileList(),
					e -> notificationPresenter.showError(e.getCaption(), e.getMessage())).open();;
			

		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getMessage());
			return;
		}
	}
	
	@Override
	protected String getConfirmDeleteText(String profiles)
	{
		return msg.getMessage("InputTranslationsView.confirmDelete", profiles);
	}

}
