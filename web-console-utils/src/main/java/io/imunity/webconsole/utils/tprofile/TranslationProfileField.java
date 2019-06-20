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

import io.imunity.webadmin.tprofile.ActionParameterComponentProvider;
import io.imunity.webadmin.tprofile.TranslationRulesPresenter;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
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
	private UnityMessageSource msg;
	private TypesRegistryBase<? extends TranslationActionFactory<?>> registry;
	private ActionParameterComponentProvider actionComponentProvider;
	private SubViewSwitcher subViewSwitcher;
	private TranslationProfile value;

	TranslationProfileField(UnityMessageSource msg,
			TypesRegistryBase<? extends TranslationActionFactory<?>> registry,
			ActionParameterComponentProvider actionComponentProvider, SubViewSwitcher subViewSwitcher)
	{
		this.msg = msg;
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

		EditTranslationProfileSubView editProfileSubView = getEditInputTranslationSubViewInstance(p -> {

			subViewSwitcher.exitSubView();
			editProfile.focus();
			doSetValue(p);
			fireEvent(new ValueChangeEvent<TranslationProfile>(this, value, true));
		}, () -> {
			subViewSwitcher.exitSubView();
			editProfile.focus();
		});

		editProfile.addClickListener(e -> {
			editProfileSubView.setInput(value.clone());
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

	public EditTranslationProfileSubView getEditInputTranslationSubViewInstance(
			Consumer<TranslationProfile> onConfirm, Runnable onCancel)
	{
		try
		{
			actionComponentProvider.init();
		} catch (EngineException e)
		{
			throw new InternalException("Can not init action provider");
		}
		return new EditTranslationProfileSubView(msg, registry, ProfileType.INPUT, actionComponentProvider,
				onConfirm, onCancel);
	}
}
