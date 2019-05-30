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
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Shows all authenticators
 * 
 * @author P.Piernik
 *
 */
public class AuthenticatorsComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private AuthenticatorsController controller;
	private GridWithActionColumn<AuthenticatorEntry> authenticatorsList;

	public AuthenticatorsComponent(UnityMessageSource msg, AuthenticatorsController controller)
	{
		this.msg = msg;
		this.controller = controller;
		initUI();
	}

	private void initUI()
	{
		HorizontalLayout buttonsBar = StandardButtonsHelper.buildTopButtonsBar(StandardButtonsHelper
				.build4AddAction(msg, e -> NavigationHelper.goToView(NewAuthenticatorView.VIEW_NAME)));

		authenticatorsList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
		authenticatorsList.addComponentColumn(
				a -> StandardButtonsHelper.buildLinkButton(a.authenticator.id, e -> gotoEdit(a)),
				msg.getMessage("AuthenticatorsComponent.nameCaption"), 10);
		authenticatorsList.addByClickDetailsComponent(authenticator -> {
			{
				Label endpoints = new Label();
				endpoints.setCaption(msg.getMessage("AuthenticatorsComponent.endpointsCaption"));
				endpoints.setValue(String.join(", ", authenticator.endpoints));
				FormLayout wrapper = new FormLayout(endpoints);
				endpoints.setStyleName(Styles.wordWrap.toString());
				wrapper.setWidth(95, Unit.PERCENTAGE);
				return wrapper;
			}
		});

		authenticatorsList.setItems(getAuthenticators());

		VerticalLayout main = new VerticalLayout();
		Label authCaption = new Label(msg.getMessage("AuthenticatorsComponent.caption"));
		authCaption.setStyleName(Styles.sectionTitle.toString());
		main.addComponent(authCaption);
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
		NavigationHelper.goToView(EditAuthenticatorView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
				+ a.authenticator.id);
	}

	private Collection<AuthenticatorEntry> getAuthenticators()
	{
		try
		{
			return controller.getAllAuthenticators();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	private void remove(AuthenticatorEntry a)
	{
		try
		{
			controller.removeAuthenticator(a.authenticator);
			authenticatorsList.removeElement(a);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void tryRemove(AuthenticatorEntry a)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(a.authenticator.id));
		new ConfirmDialog(msg, msg.getMessage("AuthenticatorsComponent.confirmDelete", confirmText),
				() -> remove(a)).show();
	}
}
