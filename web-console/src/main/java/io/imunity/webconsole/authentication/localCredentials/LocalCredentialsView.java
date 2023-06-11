/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.localCredentials;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.AuthenticationNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Lists all local credentials
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class LocalCredentialsView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "LocalCredentials";

	private LocalCredentialsController controller;
	private MessageSource msg;
	private GridWithActionColumn<CredentialDefinition> credList;
	private EventsBus bus;

	@Autowired
	public LocalCredentialsView(MessageSource msg, LocalCredentialsController controller)
	{
		this.controller = controller;
		this.msg = msg;
		this.bus = WebSession.getCurrent().getEventBus();
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		HorizontalLayout buttonsBar = StandardButtonsHelper
				.buildTopButtonsBar(StandardButtonsHelper.build4AddAction(msg,
						e -> NavigationHelper.goToView(NewLocalCredentialView.VIEW_NAME)));

		credList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
		credList.addComponentColumn(c -> StandardButtonsHelper.buildLinkButton(c.getName(), e -> {
			if (!c.isReadOnly())
				gotoEdit(c);
			else
				gotoShowDetails(c);
		}), msg.getMessage("LocalCredentialsView.nameCaption"), 10).setSortable(true)
				.setComparator((c1, c2) -> {
					return c1.getName().compareTo(c2.getName());
				}).setId("name");
		;

		credList.setItems(getCredentials());
		credList.sort("name");
		VerticalLayout main = new VerticalLayout();
		main.addComponent(buttonsBar);
		main.addComponent(credList);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);

		setCompositionRoot(main);
	}

	private Collection<CredentialDefinition> getCredentials()
	{
		try
		{
			return controller.getCredentials();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	private List<SingleActionHandler<CredentialDefinition>> getActionsHandlers()
	{
		SingleActionHandler<CredentialDefinition> show = SingleActionHandler
				.builder4ShowDetails(msg, CredentialDefinition.class)
				.withHandler(r -> gotoShowDetails(r.iterator().next()))
				.withDisabledPredicate(r -> !r.isReadOnly()).hideIfInactive()
				.build();

		SingleActionHandler<CredentialDefinition> edit = SingleActionHandler
				.builder4Edit(msg, CredentialDefinition.class)
				.withHandler(r -> gotoEdit(r.iterator().next()))
				.withDisabledPredicate(r -> r.isReadOnly()).hideIfInactive().build();

		SingleActionHandler<CredentialDefinition> remove = SingleActionHandler
				.builder4Delete(msg, CredentialDefinition.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(show, edit, remove);
	}

	private void tryRemove(CredentialDefinition cred)
	{
		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(cred.getName()));
		new ConfirmDialog(msg, msg.getMessage("LocalCredentialsView.confirmDelete", confirmText),
				() -> remove(cred)).show();

	}

	private void remove(CredentialDefinition cred)
	{
		try
		{
			controller.removeCredential(cred, bus);
			credList.removeElement(cred);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void gotoShowDetails(CredentialDefinition cred)
	{
		NavigationHelper.goToView(ShowLocalCredentialView.VIEW_NAME + "/" + CommonViewParam.name.toString()
				+ "=" + cred.getName());
	}

	private void gotoEdit(CredentialDefinition cred)
	{
		NavigationHelper.goToView(EditLocalCredentialView.VIEW_NAME + "/" + CommonViewParam.name.toString()
				+ "=" + cred.getName());
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.authentication.localCredentials");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class LocalCredentialsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		public static final String ID = VIEW_NAME;
		
		@Autowired
		public LocalCredentialsNavigationInfoProvider(MessageSource msg,
				ObjectFactory<LocalCredentialsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(ID, Type.View)
					.withParent(AuthenticationNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.authentication.localCredentials"))
					.withIcon(Images.lock.getResource())
					.withPosition(20).build());
		}
	}
}
