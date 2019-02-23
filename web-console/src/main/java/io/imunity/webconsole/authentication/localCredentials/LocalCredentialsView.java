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
import io.imunity.webelements.helpers.StandardButtonsHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn.Position;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.Column;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
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
	private UnityMessageSource msg;
	private ListOfElementsWithActions<CredentialDefinition> credList;
	private EventsBus bus;

	@Autowired
	public LocalCredentialsView(UnityMessageSource msg, LocalCredentialsController controller)
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

		credList = new ListOfElementsWithActions<>(
				Arrays.asList(new Column<>(msg.getMessage("LocalCredentialsView.nameCaption"),
						p -> StandardButtonsHelper.buildLinkButton(p.getName(),
								e -> gotoEdit(p)),
						2)),
				new ActionColumn<>(msg.getMessage("actions"), getActionsHandlers(), 0, Position.Right));

		credList.setAddSeparatorLine(true);

		for (CredentialDefinition cred : getCredentials())
		{
			credList.addEntry(cred);
		}

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
			NotificationPopup.showError(e);
		}
		return Collections.emptyList();
	}

	private List<SingleActionHandler<CredentialDefinition>> getActionsHandlers()
	{
		SingleActionHandler<CredentialDefinition> show = SingleActionHandler
				.builder4ShowDetails(msg, CredentialDefinition.class)
				.withHandler(r -> gotoShowDetails(r.iterator().next())).build();

		SingleActionHandler<CredentialDefinition> edit = SingleActionHandler
				.builder4Edit(msg, CredentialDefinition.class)
				.withHandler(r -> gotoEdit(r.iterator().next()))
				.withDisabledPredicate(r -> r.isReadOnly()).build();

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
			credList.removeEntry(cred);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private void gotoShowDetails(CredentialDefinition cred)
	{
		NavigationHelper.goToView(ShowLocalCredentialView.VIEW_NAME + "/" + CommonViewParam.name.toString()
				+ "=" + cred.getName());
		;
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

		@Autowired
		public LocalCredentialsNavigationInfoProvider(UnityMessageSource msg,
				AuthenticationNavigationInfoProvider parent,
				ObjectFactory<LocalCredentialsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.authentication.localCredentials"))
					.withPosition(1).build());

		}
	}
}
