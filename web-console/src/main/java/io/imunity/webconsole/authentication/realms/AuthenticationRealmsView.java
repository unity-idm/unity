/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.AuthenticationNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Lists all realms
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class AuthenticationRealmsView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "AuthenticationRealms";

	private AuthenticationRealmController realmsMan;
	private UnityMessageSource msg;
	private ListOfElementsWithActions<AuthenticationRealm> realmsList;

	@Autowired
	public AuthenticationRealmsView(UnityMessageSource msg,
			AuthenticationRealmController realmsMan)
	{
		this.realmsMan = realmsMan;
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		HorizontalLayout buttonsBar = new HorizontalLayout();
		buttonsBar.setMargin(false);
		Button newRealm = new Button();
		newRealm.setCaption(msg.getMessage("add"));
		newRealm.addClickListener(e -> {
			getUI().getNavigator().navigateTo(NewAuthenticationRealmView.VIEW_NAME);
		});
		buttonsBar.addComponent(newRealm);
		buttonsBar.setComponentAlignment(newRealm, Alignment.MIDDLE_RIGHT);
		buttonsBar.setWidth(100, Unit.PERCENTAGE);

		realmsList = new ListOfElementsWithActions<>(r -> new Label(r.getName()));

		SingleActionHandler<AuthenticationRealm> edit = SingleActionHandler
				.builder4Edit(msg, AuthenticationRealm.class)
				.withHandler(r -> NavigationHelper.goToView(
						EditAuthenticationRealmView.VIEW_NAME + "/"
								+ CommonViewParam.name.toString()
								+ "="
								+ r.iterator().next().getName()))
				.build();

		SingleActionHandler<AuthenticationRealm> remove = SingleActionHandler
				.builder4Delete(msg, AuthenticationRealm.class).withHandler(r -> {

					tryRemove(r.iterator().next());

				}

				).build();

		realmsList.addActionHandler(edit);
		realmsList.addActionHandler(remove);
		realmsList.setAddSeparatorLine(true);
		realmsList.addHeader(msg.getMessage("AuthenticationRealm.nameCaption"),
				msg.getMessage("actions"));

		for (AuthenticationRealm realm : getRealms())
		{
			realmsList.addEntry(realm);
		}

		VerticalLayout main = new VerticalLayout();
		main.addComponent(buttonsBar);
		main.addComponent(realmsList);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);

		setCompositionRoot(main);
	}

	private Collection<AuthenticationRealm> getRealms()
	{
		try
		{
			return realmsMan.getRealms();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		return Collections.emptyList();
	}

	private void remove(AuthenticationRealm realm)
	{
		try
		{
			if (realmsMan.removeRealm(realm))
				realmsList.removeEntry(realm);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private void tryRemove(AuthenticationRealm realm)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg,
				Sets.newHashSet(realm.getName()));
		new ConfirmDialog(msg,
				msg.getMessage("AuthenticationRealm.confirmDelete", confirmText),
				() -> remove(realm)).show();

	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.authentication.realms");
	}
	
	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class RealmsNavigationInfoProvider
			extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public RealmsNavigationInfoProvider(UnityMessageSource msg,
				AuthenticationNavigationInfoProvider parent,
				ObjectFactory<AuthenticationRealmsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withCaption(msg.getMessage(
							"WebConsoleMenu.authentication.realms"))
					.build());

		}
	}
}
