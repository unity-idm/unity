/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.utils.tprofile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.console.tprofile.ActionParameterComponentProvider;
import io.imunity.console.tprofile.TranslationProfileEditor;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.api.UnitySubView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static io.imunity.vaadin.elements.CssClassNames.EDIT_VIEW_ACTION_BUTTONS_LAYOUT;


public class EditTranslationProfileSubView extends VerticalLayout implements UnitySubView
{
	private final MessageSource msg;
	private final TranslationProfileEditor editor;
	private final Consumer<TranslationProfile> onConfirm;
	private final NotificationPresenter notificationPresenter;
	private final Runnable onCancel;

	public EditTranslationProfileSubView(MessageSource msg,
			TypesRegistryBase<? extends TranslationActionFactory<?>> registry, ProfileType type,
			ActionParameterComponentProvider actionComponentProvider,
			Consumer<TranslationProfile> onConfirm, Runnable onCancel, NotificationPresenter notificationPresenter,
			HtmlTooltipFactory htmlTooltipFactory)
	{

		this.msg = msg;
		this.onConfirm = onConfirm;
		this.onCancel = onCancel;
		this.notificationPresenter = notificationPresenter;
		editor = new TranslationProfileEditor(msg, registry, type, actionComponentProvider, notificationPresenter, htmlTooltipFactory, Set.of());
		editor.setMargin(false);
		editor.rulesOnlyMode();
		initUI();
	}

	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.add(editor);
		Button cancelButton = new Button(msg.getMessage("cancel"), event -> onCancel.run());
		cancelButton.setWidthFull();
		Button updateButton = new Button(msg.getMessage("update"), event ->
		{
			try
			{
				onConfirm.accept(editor.getProfile());
			} catch (FormValidationException e)
			{
				notificationPresenter.showError(
						msg.getMessage("EditTranslationProfileSubView.inconsistentConfiguration"),
						e.getMessage());
			}
		});
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		updateButton.setWidthFull();
		HorizontalLayout buttonsLayout = new HorizontalLayout(cancelButton, updateButton);
		buttonsLayout.setClassName(EDIT_VIEW_ACTION_BUTTONS_LAYOUT.getName());
		main.add(buttonsLayout);

		add(main);
	}

	public void setInput(TranslationProfile profile)
	{
		editor.setValue(profile);
		editor.focusFirst();
	}

	@Override
	public List<String> getBreadcrumbs()
	{
		return List.of(msg.getMessage("EditTranslationProfileSubView.breadcrumbs"), msg.getMessage("edit"));
	}

}
