/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.translation_profiles;

import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;

import io.imunity.console.tprofile.TranslationProfileEditor;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.EditViewActionLayoutFactory;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileMode;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;

public abstract class EditTranslationView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final TranslationsServiceBase controller;
	private TranslationProfileEditor editor;

	private BreadCrumbParameter breadCrumbParameter;

	protected EditTranslationView(MessageSource msg, TranslationsServiceBase controller,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;

	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String profileName)
	{
		getContent().removeAll();

		TranslationProfile profile;
		try
		{
			profile = controller.getProfile(profileName);
			breadCrumbParameter = new BreadCrumbParameter(profileName, profileName);

		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
			return;
		}

		initUI(profile);
	}

	public void initUI(TranslationProfile profile)
	{

		try
		{
			editor = getEditor(profile);
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
			UI.getCurrent()
					.navigate(getViewAll());
			return;
		}

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.add(editor);

		HorizontalLayout buttons = null;
		if (!editor.isReadOnlyMode())
		{

			buttons = EditViewActionLayoutFactory.createActionLayout(msg, true, getViewAll(), () -> onConfirm());

		} else
		{
			buttons = EditViewActionLayoutFactory.createActionLayout(msg, getViewAll());
		}

		main.add(buttons);
		getContent().add(main);
	}

	private void onConfirm()
	{

		TranslationProfile profile;
		try
		{
			profile = editor.getProfile();
		} catch (FormValidationException e)
		{
			return;
		}

		try
		{
			controller.updateProfile(profile);

		} catch (ControllerException e)
		{

			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
			return;
		}

		UI.getCurrent()
				.navigate(getViewAll());

	}

	private TranslationProfileEditor getEditor(TranslationProfile profile) throws ControllerException
	{
		editor = controller.getEditor();
		editor.setValue(profile);
		if (profile.getProfileMode()
				.equals(ProfileMode.READ_ONLY))
		{
			editor.setReadOnlyMode();
		}
		return editor;

	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	public abstract Class<? extends ConsoleViewComponent> getViewAll();
}
