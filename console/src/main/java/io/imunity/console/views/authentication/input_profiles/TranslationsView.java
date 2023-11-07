/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.input_profiles;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.RouterLink;

import io.imunity.console.utils.ObjectToJsonFileExporterHelper;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.ShowViewActionLayoutFactory;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.grid.GridWithActionColumn;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleActionHandler;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileMode;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

public abstract class TranslationsView extends ConsoleViewComponent
{
	protected final MessageSource msg;
	protected final TranslationsServiceBase controller;
	protected final NotificationPresenter notificationPresenter;
	private GridWithActionColumn<TranslationProfile> profileList;

	@Autowired
	public TranslationsView(MessageSource msg, TranslationsServiceBase controller,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;
		init();
	}

	private void init()
	{
		profileList = new GridWithActionColumn<>(msg, getActionsHandlers());
		Column<TranslationProfile> name = profileList
				.addComponentColumn(e -> new RouterLink(e.getName(), getEditView(), e.getName()))
				.setHeader(msg.getMessage("TranslationProfilesView.nameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(r -> r.getName()));

		refreshProfileList();

		VerticalLayout main = new VerticalLayout();
		NativeLabel title = new NativeLabel(getHeaderCaption());
		main.add(title);
		List<Button> buttons = getButtonsBar();
		main.add(ShowViewActionLayoutFactory.buildTopButtonsBar(buttons.toArray(new Button[buttons.size()])));
		main.add(profileList);
		main.setMargin(false);
		profileList.sort(GridSortOrder.asc(name)
				.build());
		getContent().add(main);
	}

	private List<SingleActionHandler<TranslationProfile>> getActionsHandlers()
	{
		SingleActionHandler<TranslationProfile> edit = SingleActionHandler.builder4Edit(msg, TranslationProfile.class)
				.withHandler(r -> gotoEdit(r.iterator()
						.next()))
				.build();

		SingleActionHandler<TranslationProfile> remove = SingleActionHandler
				.builder4Delete(msg, TranslationProfile.class)
				.withHandler(r -> tryRemove(r.iterator()
						.next()))
				.withDisabledPredicate(tp -> tp.getProfileMode() == ProfileMode.READ_ONLY)
				.build();

		SingleActionHandler<TranslationProfile> copy = SingleActionHandler.builder4Copy(msg, TranslationProfile.class)
				.withHandler(r -> gotoCopy(r.iterator()
						.next()))
				.build();

		SingleActionHandler<TranslationProfile> export = SingleActionHandler.builder(TranslationProfile.class)
				.withCaption(msg.getMessage("TranslationProfilesView.exportAction"))
				.withIcon(VaadinIcon.UPLOAD)
				.withHandler(r -> export(r.iterator()
						.next()))
				.build();
		return Arrays.asList(edit, copy, remove, export);
	}

	protected List<Button> getButtonsBar()
	{
		Button newProfile = ShowViewActionLayoutFactory.build4AddAction(msg, e -> UI.getCurrent()
				.navigate(getNewView()));
		return Arrays.asList(newProfile);
	}

	protected void refreshProfileList()
	{
		profileList.removeAllElements();
		profileList.setItems(getInputTranslations().stream()
				.sorted((t1, t2) -> t1.getName()
						.compareTo(t2.getName()))
				.collect(Collectors.toList()));
	}

	private Collection<TranslationProfile> getInputTranslations()
	{
		try
		{
			return controller.getProfiles();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
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
			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
		}
	}

	private void tryRemove(TranslationProfile profile)
	{
		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(profile.getName()));
		new ConfirmDialog(msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("TranslationProfilesView.confirmDelete", confirmText), msg.getMessage("ok"),
				e -> remove(profile), msg.getMessage("cancel"), e ->
				{
				}).open();

	}

	private void export(TranslationProfile profile)
	{
		ObjectToJsonFileExporterHelper.export(getContent(), Set.of(profile), profile.getName() + ".json");
	}

	private void gotoEdit(TranslationProfile profile)
	{

		UI.getCurrent()
				.navigate(getEditView(), profile.getName());
	}

	private void gotoCopy(TranslationProfile profile)
	{

		UI.getCurrent()
				.navigate(getNewView(), profile.getName());
	}

	protected abstract <T, C extends Component & HasUrlParameter<T>> Class<? extends C> getEditView();

	protected abstract <T, C extends Component & HasUrlParameter<T>> Class<? extends C> getNewView();

	protected abstract String getHeaderCaption();
}
