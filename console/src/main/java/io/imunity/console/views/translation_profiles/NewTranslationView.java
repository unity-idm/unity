/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.translation_profiles;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.EditViewActionLayoutFactory;
import io.imunity.console.tprofile.TranslationProfileEditor;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.Optional;

public abstract class NewTranslationView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final TranslationsServiceBase controller;
	private TranslationProfileEditor editor;
	private BreadCrumbParameter breadCrumbParameter;

	@Autowired
	protected NewTranslationView(MessageSource msg, TranslationsServiceBase controller,
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
		breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("new"));
		TranslationProfile profile = null;

		if (profileName != null)
		{
			try
			{
				profile = controller.getProfile(profileName);

			} catch (ControllerException e)
			{
				notificationPresenter.showError(e.getCaption(), e.getCause()
						.getMessage());
				return;
			}
		}
		initUI(profile);
	}

	private void initUI(TranslationProfile toClone)
	{
		try
		{
			editor = getEditor(toClone);
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
		main.add(EditViewActionLayoutFactory.createActionLayout(msg, false, getViewAll(), () -> onConfirm()));
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
			controller.addProfile(profile);

		} catch (ControllerException e)
		{

			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
			return;
		}

		UI.getCurrent()
				.navigate(getViewAll());

	}

	private TranslationProfileEditor getEditor(TranslationProfile toClone) throws ControllerException
	{
		TranslationProfileEditor editor = controller.getEditor();
		if (toClone != null)
		{
			editor.setValue(toClone);
			editor.setCopyMode();
		}
		return editor;
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	public abstract  Class<? extends ConsoleViewComponent>  getViewAll();
}
