/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.inputTranslation;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.ui.Button;

import io.imunity.webconsole.UnityViewWithSandbox;
import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.AuthenticationNavigationInfoProvider;
import io.imunity.webconsole.translationProfile.TranslationsView;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;
import pl.edu.icm.unity.webui.sandbox.wizard.SandboxWizardDialog;

/**
 * Lists all input translation profiles
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class InputTranslationsView extends TranslationsView implements UnityViewWithSandbox
{
	public static final String VIEW_NAME = "InputTranslations";
	private SandboxAuthnRouter sandBoxRouter;

	@Autowired
	public InputTranslationsView(MessageSource msg, InputTranslationsController controller)
	{
		super(msg, controller);
	}

	@Override
	protected String getEditView()
	{
		return EditInputTranslationView.VIEW_NAME;
	}

	@Override
	protected String getNewView()
	{
		return NewInputTranslationView.VIEW_NAME;
	}

	@Override
	public String getHeaderCaption()
	{
		return msg.getMessage("InputTranslationsView.headerCaption");
	}

	protected List<Button> getButtonsBar()
	{
		Button wizard = StandardButtonsHelper.buildActionButton(
				msg.getMessage("InputTranslationsView.wizard"), Images.wizard,
				e -> showWizardDialog());
		return Stream.concat(Stream.of(wizard), super.getButtonsBar().stream()).collect(Collectors.toList());
	}

	private void showWizardDialog()
	{
		SandboxWizardDialog wizardDialog;
		try
		{
			wizardDialog = ((InputTranslationsController) controller).getWizardDialog(sandBoxRouter,
					() -> refreshProfileList(), e -> NotificationPopup.showError(msg, e));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return;
		}
		wizardDialog.show();
	}

	@Override
	public String getDisplayedName()
	{

		return msg.getMessage("WebConsoleMenu.authentication.inputTranslation");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public void setSandboxRouter(SandboxAuthnRouter router)
	{
		sandBoxRouter = router;

	}

	@Component
	public static class InputTranslationsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		public static final String ID = VIEW_NAME;
		
		@Autowired
		public InputTranslationsNavigationInfoProvider(MessageSource msg,
				ObjectFactory<InputTranslationsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(ID, Type.View)
					.withParent(AuthenticationNavigationInfoProvider.ID).withObjectFactory(factory)
					.withIcon(Images.download.getResource())
					.withCaption(msg.getMessage("WebConsoleMenu.authentication.inputTranslation"))
					.withPosition(50).build());

		}
	}

}
