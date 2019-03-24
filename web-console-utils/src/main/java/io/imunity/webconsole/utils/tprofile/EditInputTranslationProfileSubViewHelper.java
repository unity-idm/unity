/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.utils.tprofile;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webadmin.tprofile.ActionParameterComponentProvider;
import io.imunity.webadmin.tprofile.TranslationRulesPresenter;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
public class EditInputTranslationProfileSubViewHelper
{
	private UnityMessageSource msg;
	private TypesRegistryBase<? extends TranslationActionFactory<?>> registry;
	private ActionParameterComponentProvider actionComponentProvider;

	@Autowired
	EditInputTranslationProfileSubViewHelper(UnityMessageSource msg,
			InputTranslationActionsRegistry inputActionsRegistry,
			ActionParameterComponentProvider actionComponentProvider)
	{

		this.msg = msg;
		this.registry = inputActionsRegistry;
		this.actionComponentProvider = actionComponentProvider;
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

	public TranslationRulesPresenter getRulesPresenterInstance()
	{
		return new TranslationRulesPresenter(msg, registry);
	}

	public CollapsibleLayout buildRemoteDataMappingEditorSection(SubViewSwitcher subViewSwitcher,
			TranslationRulesPresenter presenter,
			Consumer<TranslationProfile> profileSetter, Supplier<TranslationProfile> profileGetter)
	{
		VerticalLayout viewerWrapper = new VerticalLayout();
		viewerWrapper.setMargin(false);
		viewerWrapper.setSpacing(false);
		HorizontalLayout profileBar = new HorizontalLayout();
		profileBar.setMargin(false);
		Label profile = new Label(msg.getMessage("RemoteDataMapping.translationProfile"));
		profileBar.addComponent(profile);
		Button editProfile = new Button();

		EditTranslationProfileSubView editProfileSubView = getEditInputTranslationSubViewInstance(p -> {
			profileSetter.accept(p);
			subViewSwitcher.exitSubView();
			editProfile.focus();
			presenter.setInput(p.getRules());
		}, () -> {
			subViewSwitcher.exitSubView();
			editProfile.focus();
		});

		editProfile.addClickListener(e -> {
			editProfileSubView.setInput(profileGetter.get());
			subViewSwitcher.goToSubView(editProfileSubView);

		});

		editProfile.setIcon(Images.edit.getResource());
		editProfile.setStyleName(Styles.vButtonBorderless.toString());
		editProfile.setStyleName(Styles.vButtonLink.toString());
		profileBar.addComponent(editProfile);

		viewerWrapper.addComponent(profileBar);
		viewerWrapper.addComponent(presenter);
		return new CollapsibleLayout(msg.getMessage("RemoteDataMapping.caption"),
				viewerWrapper);
	}
}
