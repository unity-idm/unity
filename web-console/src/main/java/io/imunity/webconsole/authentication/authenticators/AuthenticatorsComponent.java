/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.authentication.authenticators;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Sets;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
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
 * Shows all authenticators
 * @author P.Piernik
 *
 */
public class AuthenticatorsComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private AuthenticatorsController controller;
	private ListOfElementsWithActions<AuthenticatorEntry> authenticatorsList;
	
	public AuthenticatorsComponent(UnityMessageSource msg, AuthenticatorsController controller)
	{
		this.msg = msg;
		this.controller = controller;
		initUI();
	}

	private void initUI()
	{
		HorizontalLayout buttonsBar = StandardButtonsHelper
				.buildTopButtonsBar(StandardButtonsHelper.build4AddAction(msg,
						e -> NavigationHelper.goToView(NewAuthenticatorView.VIEW_NAME)));

		authenticatorsList = new ListOfElementsWithActions<>(Arrays.asList(
				new Column<>(msg.getMessage("AuthenticatorsComponent.nameCaption"),
						a -> StandardButtonsHelper.buildLinkButton( a.authneticator.id,
								e -> gotoEdit(a)),
						1),
				new Column<>(msg.getMessage("AuthenticatorsComponent.endpointsCaption"),
						r -> new Label(String.join(", ", r.endpoints)), 4)),
				new ActionColumn<>(msg.getMessage("actions"), getActionsHandlers(), 0, Position.Right));

		authenticatorsList.setAddSeparatorLine(true);

		for (AuthenticatorEntry authenticator : getAutheticators())
		{
			authenticatorsList.addEntry(authenticator);
		}

		VerticalLayout main = new VerticalLayout();
		Label trustedCertCaption = new Label(msg.getMessage("AuthenticatorsComponent.caption"));
		trustedCertCaption.setStyleName(Styles.bold.toString());
		main.addComponent(trustedCertCaption);
		main.addComponent(buttonsBar);
		main.addComponent(authenticatorsList);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		setCompositionRoot(main);
	}

	private List<SingleActionHandler<AuthenticatorEntry>> getActionsHandlers()
	{
		SingleActionHandler<AuthenticatorEntry> edit = SingleActionHandler
				.builder4Edit(msg, AuthenticatorEntry.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		SingleActionHandler<AuthenticatorEntry> remove = SingleActionHandler
				.builder4Delete(msg, AuthenticatorEntry.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(edit, remove);

	}

	private void gotoEdit(AuthenticatorEntry a)
	{
		NavigationHelper.goToView(EditAuthenticatorView.VIEW_NAME + "/" + CommonViewParam.name.toString()
				+ "=" + a.authneticator.id);
	}

	private Collection<AuthenticatorEntry> getAutheticators()
	{
		try
		{
			return controller.getAllAuthenticators();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		return Collections.emptyList();
	}

	private void remove(AuthenticatorEntry a)
	{
		try
		{
			controller.removeAuthenticator(a.authneticator);
			authenticatorsList.removeEntry(a);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private void tryRemove(AuthenticatorEntry a)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(a.authneticator.id));
		new ConfirmDialog(msg, msg.getMessage("AuthenticatorsComponent.confirmDelete", confirmText),
				() -> remove(a)).show();
	}
}
