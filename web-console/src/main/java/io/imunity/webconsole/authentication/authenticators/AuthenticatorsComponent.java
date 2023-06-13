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

import io.imunity.webconsole.translationProfile.dryrun.DryRunWizardProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;
import pl.edu.icm.unity.webui.sandbox.wizard.SandboxWizardDialog;

/**
 * Shows all authenticators
 * 
 * @author P.Piernik
 *
 */
public class AuthenticatorsComponent extends CustomComponent
{
	private MessageSource msg;
	private AuthenticatorsController controller;
	private GridWithActionColumn<AuthenticatorEntry> authenticatorsGrid;
	private SandboxAuthnRouter sandBoxRouter;

	public AuthenticatorsComponent(MessageSource msg, AuthenticatorsController controller,
			SandboxAuthnRouter sandBoxRouter)
	{
		this.msg = msg;
		this.controller = controller;
		this.sandBoxRouter = sandBoxRouter;
		initUI();
	}

	private void initUI()
	{
		HorizontalLayout buttonsBar = StandardButtonsHelper.buildTopButtonsBar(
				StandardButtonsHelper.buildButton(msg.getMessage("AuthenticatorsComponent.test"), 
						Images.dryrun, e -> {
					DryRunWizardProvider dryRunProvider = null;
					try
					{
						dryRunProvider = controller.getDryRunProvider(sandBoxRouter);
					} catch (ControllerException e1)
					{
						NotificationPopup.showError(msg, e1);
						return;
					}
					SandboxWizardDialog dialog = new SandboxWizardDialog(
							dryRunProvider.getWizardInstance(),
							dryRunProvider.getCaption());
					dialog.show();
				}), 
				StandardButtonsHelper.build4AddAction(msg,
						e -> NavigationHelper.goToView(NewAuthenticatorView.VIEW_NAME)));

		authenticatorsGrid = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
		authenticatorsGrid.addShowDetailsColumn(a -> getDetailsComponent(a));
		authenticatorsGrid
				.addComponentColumn(
						a -> StandardButtonsHelper.buildLinkButton(a.authenticator.id,
								e -> gotoEdit(a)),
						msg.getMessage("AuthenticatorsComponent.nameCaption"), 10)
				.setSortable(true).setComparator((a1, a2) -> {
					return a1.authenticator.id.compareTo(a2.authenticator.id);
				}).setId("name");
		;
		authenticatorsGrid.setItems(getAuthenticators());
		authenticatorsGrid.sort("name");
		authenticatorsGrid.setHeightByRows(false);
		authenticatorsGrid.setHeight(100, Unit.PERCENTAGE);

		VerticalLayout main = new VerticalLayout();
		Label authCaption = new Label(msg.getMessage("AuthenticatorsComponent.caption"));
		authCaption.setStyleName(Styles.sectionTitle.toString());
		main.addComponent(authCaption);
		main.addComponent(buttonsBar);
		main.addComponent(authenticatorsGrid);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		setCompositionRoot(main);
	}

	private FormLayout getDetailsComponent(AuthenticatorEntry authenticator)
	{
		Label endpoints = new Label();
		endpoints.setCaption(msg.getMessage("AuthenticatorsComponent.endpointsCaption"));
		endpoints.setValue(String.join(", ", authenticator.endpoints));
		FormLayout wrapper = new FormLayout(endpoints);
		endpoints.setStyleName(Styles.wordWrap.toString());
		wrapper.setWidth(95, Unit.PERCENTAGE);
		return wrapper;
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
			authenticatorsGrid.removeElement(a);
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
