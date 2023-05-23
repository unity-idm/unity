/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.views.trusted_device;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Route;
import io.imunity.home.views.HomeUiMenu;
import io.imunity.home.views.HomeViewComponent;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.RememberMeProcessor;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.types.basic.EntityParam;

import javax.annotation.security.PermitAll;
import java.util.List;
import java.util.stream.Collectors;

@PermitAll
@Route(value = "/trusted-device", layout = HomeUiMenu.class)
public class TrustedDeviceView extends HomeViewComponent
{
	private final TokensManagement tokenMan;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	TrustedDeviceView(TokensManagement tokenMan, MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.tokenMan = tokenMan;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	private AccordionPanel createPanel(TrustedDeviceModel model)
	{
		AccordionPanel accordionPanel = new AccordionPanel();
		H3 title = new H3( msg.getMessage("TrustedDevice.deviceWith", model.getOS(), model.getBrowser()));
		title.getStyle().set("margin", "0");
		Button button = new Button(msg.getMessage("TrustedDevice.revoke"), e ->
		{
			tokenMan.removeToken(model.getToken().getType(), model.getToken().getValue());
			refresh();
		});
		HorizontalLayout summary = new HorizontalLayout(title, button);
		summary.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		summary.setAlignItems(FlexComponent.Alignment.CENTER);
		summary.setPadding(false);
		summary.setWidthFull();
		accordionPanel.setSummary(summary);

		FormLayout content = new FormLayout();
		content.addFormItem(new Label(model.getIp()), msg.getMessage("RememberMeToken.ip"));
		content.addFormItem(new Label(model.getPolicy()), msg.getMessage("RememberMeToken.policy"));
		content.addFormItem(new Label(model.getCreateTime()), msg.getMessage("RememberMeToken.createTime"));
		content.addFormItem(new Label(model.getExpires()), msg.getMessage("RememberMeToken.expires"));
		content.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		accordionPanel.setContent(content);

		return accordionPanel;
	}

	private List<TrustedDeviceModel> getTrustedDevices()
	{
		LoginSession theUser = InvocationContext.getCurrent().getLoginSession();
		try
		{
			return tokenMan.getOwnedTokens(RememberMeProcessor.REMEMBER_ME_TOKEN_TYPE, new EntityParam(theUser.getEntityId()))
				.stream().map(t -> new TrustedDeviceModel(t, msg))
					.collect(Collectors.toList());
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("TrustedDevicesComponent.errorGetTokens"), e.getMessage());
			return List.of();
		}

	}

	@Override
	public void afterNavigation(AfterNavigationEvent event)
	{
		refresh();
	}

	private void refresh()
	{
		getContent().removeAll();
		Accordion accordion = new Accordion();
		accordion.setWidthFull();
		getTrustedDevices().stream()
				.map(this::createPanel)
				.forEach(accordion::add);
		accordion.close();
		H2 title = new H2(msg.getMessage("UserHomeUI.trustedDevices"));
		title.getStyle().set("margin", "0");
		getContent().add(new VerticalLayout(title, accordion));
	}
}
