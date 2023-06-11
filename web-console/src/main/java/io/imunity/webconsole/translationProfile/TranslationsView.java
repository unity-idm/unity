/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.translationProfile;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.simplefiledownloader.SimpleFileDownloader;

import com.google.common.collect.Sets;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileMode;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

public abstract class TranslationsView extends CustomComponent implements UnityView
{
	protected MessageSource msg;
	protected TranslationsControllerBase controller;
	private GridWithActionColumn<TranslationProfile> profileList;

	@Autowired
	public TranslationsView(MessageSource msg, TranslationsControllerBase controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		profileList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
		profileList.addComponentColumn(
				c -> StandardButtonsHelper.buildLinkButton(c.getName(), e -> gotoEdit(c)),
				msg.getMessage("TranslationProfilesView.nameCaption"), 10).setSortable(true)
				.setComparator((c1, c2) -> {
					return c1.getName().compareTo(c2.getName());
				}).setId("name");
		profileList.sort("name");

		refreshProfileList();

		VerticalLayout main = new VerticalLayout();
		Label title = new Label(getHeaderCaption());
		title.setStyleName(Styles.sectionTitle.toString());
		main.addComponent(title);
		List<Button> buttons = getButtonsBar();
		main.addComponent(
				StandardButtonsHelper.buildTopButtonsBar(buttons.toArray(new Button[buttons.size()])));
		main.addComponent(profileList);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);

		setCompositionRoot(main);
	}

	private List<SingleActionHandler<TranslationProfile>> getActionsHandlers()
	{
		SingleActionHandler<TranslationProfile> edit = SingleActionHandler
				.builder4Edit(msg, TranslationProfile.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		SingleActionHandler<TranslationProfile> remove = SingleActionHandler
				.builder4Delete(msg, TranslationProfile.class)
				.withHandler(r -> tryRemove(r.iterator().next()))
				.withDisabledPredicate(tp -> tp.getProfileMode() == ProfileMode.READ_ONLY).build();

		SingleActionHandler<TranslationProfile> copy = SingleActionHandler
				.builder4Copy(msg, TranslationProfile.class)
				.withHandler(r -> gotoCopy(r.iterator().next())).build();

		SingleActionHandler<TranslationProfile> export = SingleActionHandler.builder(TranslationProfile.class)
				.withCaption(msg.getMessage("TranslationProfilesView.exportAction"))
				.withIcon(Images.export.getResource()).withHandler(r -> export(r.iterator().next()))
				.build();
		return Arrays.asList(edit, copy, remove, export);
	}

	protected List<Button> getButtonsBar()
	{
		Button newProfile = StandardButtonsHelper.build4AddAction(msg,
				e -> NavigationHelper.goToView(getNewView()));
		return Arrays.asList(newProfile);
	}

	protected void refreshProfileList()
	{
		profileList.removeAllElements();
		for (TranslationProfile profile : getInputTranslations().stream()
				.sorted((t1, t2) -> t1.getName().compareTo(t2.getName())).collect(Collectors.toList()))
		{
			profileList.addElement(profile);
		}
	}

	private Collection<TranslationProfile> getInputTranslations()
	{
		try
		{
			return controller.getProfiles();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	private void remove(TranslationProfile profile)
	{
		try
		{
			controller.removeProfile(profile);
			profileList.removeElement(profile);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void tryRemove(TranslationProfile profile)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(profile.getName()));
		new ConfirmDialog(msg, msg.getMessage("TranslationProfilesView.confirmDelete", confirmText),
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
			NotificationPopup.showError(msg, e);
			return;
		}
		addExtension(downloader);
		downloader.download();
	}

	private void gotoEdit(TranslationProfile profile)
	{
		NavigationHelper.goToView(
				getEditView() + "/" + CommonViewParam.name.toString() + "=" + profile.getName());
	}

	private void gotoCopy(TranslationProfile profile)
	{
		NavigationHelper.goToView(
				getNewView() + "/" + CommonViewParam.name.toString() + "=" + profile.getName());
	}

	protected abstract String getEditView();
	protected abstract String getNewView();
	public abstract String getDisplayedName();
	public abstract String getViewName();
	protected abstract String getHeaderCaption();
}
