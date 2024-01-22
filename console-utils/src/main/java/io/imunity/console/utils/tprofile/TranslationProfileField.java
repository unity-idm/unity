/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.utils.tprofile;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.console.tprofile.ActionParameterComponentProvider;
import io.imunity.console.tprofile.TranslationRulesPresenter;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

import java.util.List;
import java.util.function.Consumer;

import static io.imunity.vaadin.elements.CSSVars.BASE_MARGIN;
import static io.imunity.vaadin.elements.CssClassNames.POINTER;


public class TranslationProfileField extends CustomField<TranslationProfile>
{
	private final TranslationRulesPresenter viewer;
	private final MessageSource msg;
	private final TypesRegistryBase<? extends TranslationActionFactory<?>> registry;
	private final ActionParameterComponentProvider actionComponentProvider;
	private final ProfileType type;
	private final SubViewSwitcher subViewSwitcher;
	private final NotificationPresenter notificationPresenter;
	private final HtmlTooltipFactory htmlTooltipFactory;
	private TranslationProfile value;

	TranslationProfileField(MessageSource msg, ProfileType type,
			TypesRegistryBase<? extends TranslationActionFactory<?>> registry,
			ActionParameterComponentProvider actionComponentProvider, SubViewSwitcher subViewSwitcher,
			NotificationPresenter notificationPresenter, HtmlTooltipFactory htmlTooltipFactory)
	{
		this.msg = msg;
		this.type = type;
		this.registry = registry;
		this.actionComponentProvider = actionComponentProvider;
		this.subViewSwitcher = subViewSwitcher;
		this.notificationPresenter = notificationPresenter;
		this.htmlTooltipFactory = htmlTooltipFactory;
		this.viewer = new TranslationRulesPresenter(msg, registry);
		initContent();
	}

	@Override
	public TranslationProfile getValue()
	{
		return value;
	}

	protected void initContent()
	{
		VerticalLayout viewerWrapper = new VerticalLayout();
		viewerWrapper.setPadding(false);
		viewerWrapper.setSpacing(false);
		HorizontalLayout profileBar = new HorizontalLayout();
		profileBar.setAlignItems(FlexComponent.Alignment.CENTER);
		profileBar.getStyle().set("margin-left", BASE_MARGIN.value());
		Span profile = new Span(msg.getMessage("TranslationProfileField.translationProfile"));
		profileBar.add(profile);
		Icon editProfile = VaadinIcon.EDIT.create();
		editProfile.addClassName(POINTER.getName());

		EditTranslationProfileSubView editProfileSubView = getEditTranslationProfileSubViewInstance(p ->
		{
			subViewSwitcher.exitSubViewAndShowUpdateInfo();
			setPresentationValue(p);
			fireEvent(new ComponentValueChangeEvent<>(this, this, value, false));
		}, subViewSwitcher::exitSubView);

		editProfile.addClickListener(e ->
		{
			editProfileSubView.setInput(value != null ? value.clone()
					: type.equals(ProfileType.INPUT)
					? TranslationProfileGenerator
					.generateEmbeddedEmptyInputProfile()
					: TranslationProfileGenerator
					.generateEmbeddedEmptyOutputProfile());
			subViewSwitcher.goToSubView(editProfileSubView);
		});
		profileBar.add(editProfile);

		viewerWrapper.add(profileBar);
		viewerWrapper.add(viewer);
		add(viewerWrapper);
	}

	public EditTranslationProfileSubView getEditTranslationProfileSubViewInstance(
			Consumer<TranslationProfile> onConfirm, Runnable onCancel)
	{
		try
		{
			actionComponentProvider.init();
		} catch (EngineException e)
		{
			throw new InternalException("Can not init action provider");
		}
		return new EditTranslationProfileSubView(msg, registry, type, actionComponentProvider,
				onConfirm, onCancel, notificationPresenter, htmlTooltipFactory);
	}

	@Override
	protected TranslationProfile generateModelValue()
	{
		return value;
	}

	@Override
	protected void setPresentationValue(TranslationProfile translationProfile)
	{
		this.value = translationProfile;
		viewer.setInput(value != null ? value.getRules() : List.of());
	}
}
