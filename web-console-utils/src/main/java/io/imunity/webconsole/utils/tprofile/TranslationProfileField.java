/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.utils.tprofile;

import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.tprofile.ActionParameterComponentProviderV8;
import io.imunity.webconsole.tprofile.TranslationRulesPresenter;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * Shows translation profile rules and edit button which redirects to the
 * {@link EditTranslationProfileSubView}
 * 
 * @author P.Piernik
 *
 */
public class TranslationProfileField extends CustomField<TranslationProfile>
{
	private TranslationRulesPresenter viewer;
	private MessageSource msg;
	private TypesRegistryBase<? extends TranslationActionFactory<?>> registry;
	private ActionParameterComponentProviderV8 actionComponentProvider;
	private ProfileType type;
	private SubViewSwitcher subViewSwitcher;
	private TranslationProfile value;

	TranslationProfileField(MessageSource msg, ProfileType type,
							TypesRegistryBase<? extends TranslationActionFactory<?>> registry,
							ActionParameterComponentProviderV8 actionComponentProvider, SubViewSwitcher subViewSwitcher)
	{
		this.msg = msg;
		this.type = type;
		this.registry = registry;
		this.actionComponentProvider = actionComponentProvider;
		this.subViewSwitcher = subViewSwitcher;
		this.viewer = new TranslationRulesPresenter(msg, registry);
	}

	@Override
	public TranslationProfile getValue()
	{
		return value;
	}

	@Override
	protected com.vaadin.ui.Component initContent()
	{
		VerticalLayout viewerWrapper = new VerticalLayout();
		viewerWrapper.setMargin(false);
		viewerWrapper.setSpacing(false);
		HorizontalLayout profileBar = new HorizontalLayout();
		profileBar.setMargin(false);
		Label profile = new Label(msg.getMessage("TranslationProfileField.translationProfile"));
		profileBar.addComponent(profile);
		Button editProfile = new Button();

		EditTranslationProfileSubView editProfileSubView = getEditTranslationProfileSubViewInstance(p -> {

			subViewSwitcher.exitSubViewAndShowUpdateInfo();
			editProfile.focus();
			doSetValue(p);
			fireEvent(new ValueChangeEvent<TranslationProfile>(this, value, true));
		}, () -> {
			subViewSwitcher.exitSubView();
			editProfile.focus();
		});

		editProfile.addClickListener(e -> {
			editProfileSubView.setInput(value != null ? value.clone()
					: type.equals(ProfileType.INPUT)
							? TranslationProfileGenerator
									.generateEmbeddedEmptyInputProfile()
							: TranslationProfileGenerator
									.generateEmbeddedEmptyOutputProfile());
			subViewSwitcher.goToSubView(editProfileSubView);

		});

		editProfile.setIcon(Images.edit.getResource());
		editProfile.setStyleName(Styles.vButtonBorderless.toString());
		editProfile.setStyleName(Styles.vButtonLink.toString());
		profileBar.addComponent(editProfile);

		viewerWrapper.addComponent(profileBar);
		viewerWrapper.addComponent(viewer);
		return viewerWrapper;
	}

	@Override
	protected void doSetValue(TranslationProfile value)
	{
		this.value = value;
		viewer.setInput(value != null ? value.getRules() : Lists.newArrayList());

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
				onConfirm, onCancel);
	}
}
