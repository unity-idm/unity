/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.inputTranslation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vaadin.simplefiledownloader.SimpleFileDownloader;

import com.google.common.collect.Sets;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.UnityViewWithSandbox;
import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.AuthenticationNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn.Position;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.Column;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.exceptions.ControllerException;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.webui.sandbox.wizard.SandboxWizardDialog;

/**
 * Lists all input translation profiles
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class InputTranslationsView extends CustomComponent implements UnityViewWithSandbox
{
	public static final String VIEW_NAME = "InputTranslations";

	private UnityMessageSource msg;
	private InputTranslationsController controller;
	private ListOfElementsWithActions<TranslationProfile> profileList;
	private SandboxAuthnRouter sandBoxRouter;

	@Autowired
	public InputTranslationsView(UnityMessageSource msg, InputTranslationsController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		profileList = new ListOfElementsWithActions<>(
				Arrays.asList(new Column<>(msg.getMessage("InputTranslationProfilesView.nameCaption"),
						p -> StandardButtonsHelper.buildLinkButton(p.getName(), e -> gotoEdit(p)), 2)),
				new ActionColumn<>(msg.getMessage("actions"), getActionsHandlers(),
						0, Position.Right));

		profileList.setAddSeparatorLine(true);
		
		refreshProfileList();
		
		VerticalLayout main = new VerticalLayout();
		main.addComponent(getButtonsBar());
		main.addComponent(profileList);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);

		setCompositionRoot(main);
	}
	
	private List<SingleActionHandler<TranslationProfile>> getActionsHandlers()
	{
		SingleActionHandler<TranslationProfile> edit = SingleActionHandler
				.builder4Edit(msg, TranslationProfile.class)
				.withHandler(r -> gotoEdit(r.iterator().next()))
				.build();

		SingleActionHandler<TranslationProfile> remove = SingleActionHandler
				.builder4Delete(msg, TranslationProfile.class)
				.withHandler(r -> tryRemove(r.iterator().next()))
				.withDisabledPredicate(tp -> tp.getProfileMode() == ProfileMode.READ_ONLY).build();

		SingleActionHandler<TranslationProfile> copy = SingleActionHandler
				.builder4Copy(msg, TranslationProfile.class)
				.withHandler(r -> gotoCopy(r.iterator().next())).build();

		SingleActionHandler<TranslationProfile> export = SingleActionHandler.builder(TranslationProfile.class)
				.withCaption(msg.getMessage("InputTranslationProfilesView.exportAction"))
				.withIcon(Images.export.getResource()).withHandler(r -> export(r.iterator().next()))
				.build();
		return  Arrays.asList(edit, copy, remove, export);
	}
	
	private HorizontalLayout getButtonsBar()
	{

		Button newProfile = StandardButtonsHelper.build4AddAction(msg,
				e -> NavigationHelper.goToView(NewInputTranslationView.VIEW_NAME));

		Button wizard = StandardButtonsHelper.buildActionButton(
				msg.getMessage("InputTranslationProfilesView.wizard"), Images.wizard,
				e -> showWizardDialog());

		Button dryRun = StandardButtonsHelper.buildActionButton(
				msg.getMessage("InputTranslationProfilesView.dryRun"), Images.dryrun,
				e -> showDryRunDialog());

		return StandardButtonsHelper.buildTopButtonsBar(dryRun, wizard, newProfile);
	}
	
	private void refreshProfileList()
	{
		profileList.clearContents();
		for (TranslationProfile profile : getInputTranslations().stream()
				.sorted((t1, t2) -> t1.getName().compareTo(t2.getName())).collect(Collectors.toList()))
		{
			profileList.addEntry(profile);
		}
	}

	private void showDryRunDialog()
	{
		SandboxWizardDialog dryRunWizardDialog;
		try
		{
			dryRunWizardDialog = controller.getDryRunWizardDialog(sandBoxRouter);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}
		dryRunWizardDialog.show();
		
	}

	private void showWizardDialog()
	{
		SandboxWizardDialog wizardDialog;
		try
		{
			wizardDialog = controller.getWizardDialog(sandBoxRouter, () -> refreshProfileList(),
					e -> NotificationPopup.showError(e));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}
		wizardDialog.show();
	}

	private Collection<TranslationProfile> getInputTranslations()
	{
		try
		{
			return controller.getProfiles();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		return Collections.emptyList();
	}
	
	private void remove(TranslationProfile profile)
	{
		try
		{
			controller.removeProfile(profile);
			profileList.removeEntry(profile);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private void tryRemove(TranslationProfile profile)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(profile.getName()));
		new ConfirmDialog(msg, msg.getMessage("InputTranslationProfilesView.confirmDelete", confirmText),
				() -> remove(profile)).show();
	}

	private void export(TranslationProfile profile)
	{
		SimpleFileDownloader downloader;
		try
		{
			downloader = controller.getDownloader(profile);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}
		addExtension(downloader);
		downloader.download();
	}

	private void gotoEdit(TranslationProfile profile)
	{
		NavigationHelper.goToView(EditInputTranslationView.VIEW_NAME + "/" + CommonViewParam.name.toString()
				+ "=" + profile.getName());
	}

	private void gotoCopy(TranslationProfile profile)
	{
		NavigationHelper.goToView(NewInputTranslationView.VIEW_NAME + "/" + CommonViewParam.name.toString()
				+ "=" + profile.getName());
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
		@Autowired
		public InputTranslationsNavigationInfoProvider(UnityMessageSource msg,
				AuthenticationNavigationInfoProvider parent,
				ObjectFactory<InputTranslationsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.authentication.inputTranslation"))
					.withPosition(2)
					.build());

		}
	}

	
}
