/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import java.util.Arrays;
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
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn.Position;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.Column;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
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
	private ListOfElementsWithActions<AuthenticationRealmEntry> realmsList;
	
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
		newRealm.addStyleName("u-button-action");
		newRealm.addClickListener(e -> {
			getUI().getNavigator().navigateTo(NewAuthenticationRealmView.VIEW_NAME);
		});
		buttonsBar.addComponent(newRealm);
		buttonsBar.setComponentAlignment(newRealm, Alignment.MIDDLE_RIGHT);
		buttonsBar.setWidth(100, Unit.PERCENTAGE);
		
		SingleActionHandler<AuthenticationRealmEntry> edit = SingleActionHandler
				.builder4Edit(msg, AuthenticationRealmEntry.class)
				.withHandler(r -> gotoEdit(r.iterator().next()))
				.build();

		SingleActionHandler<AuthenticationRealmEntry> remove = SingleActionHandler
				.builder4Delete(msg, AuthenticationRealmEntry.class).withHandler(r -> {

					tryRemove(r.iterator().next());

				}

				).build();
		
		realmsList = new ListOfElementsWithActions<>(
				Arrays.asList(new Column<>(msg.getMessage("AuthenticationRealm.nameCaption"),
						r -> getEditButton(r), 1),
						new Column<>(msg.getMessage("AuthenticationRealm.endpointsCaption"),
								r -> new Label(String.join(", ", r.endpoints)), 4)),
				new ActionColumn<>(msg.getMessage("actions"), Arrays.asList(edit, remove), 0,
						Position.Right)
		);
		
		realmsList.setAddSeparatorLine(true);

		for (AuthenticationRealmEntry realm : getRealms())
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

	
	private void gotoEdit(AuthenticationRealmEntry e)
	{
		NavigationHelper.goToView(
				EditAuthenticationRealmView.VIEW_NAME + "/"
						+ CommonViewParam.name.toString()
						+ "="
						+ e.realm.getName());
	}
	
	private HorizontalLayout getEditButton(AuthenticationRealmEntry e)
	{
		HorizontalLayout layout = new HorizontalLayout();
		layout.setSpacing(false);
		layout.setMargin(false);
		layout.setWidth(100, Unit.PERCENTAGE);
		Button button = new Button();
		button.setCaption(e.realm.getName());		
		button.addStyleName(Styles.vButtonLink.toString());
		button.addStyleName(Styles.vBorderLess.toString());
		button.addClickListener(ev -> gotoEdit(e));
		layout.addComponent(button);
		layout.setComponentAlignment(button, Alignment.TOP_LEFT);
		return layout;
	}
	
	private Collection<AuthenticationRealmEntry> getRealms()
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

	private void remove(AuthenticationRealmEntry realm)
	{
		try
		{
			if (realmsMan.removeRealm(realm.realm))
				realmsList.removeEntry(realm);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private void tryRemove(AuthenticationRealmEntry realm)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg,
				Sets.newHashSet(realm.realm.getName()));
		new ConfirmDialog(msg,
				msg.getMessage("AuthenticationRealmsView.confirmDelete", confirmText),
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
