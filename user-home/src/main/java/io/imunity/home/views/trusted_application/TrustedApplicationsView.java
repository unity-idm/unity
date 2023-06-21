/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.views.trusted_application;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.imunity.home.views.HomeUiMenu;
import io.imunity.home.views.HomeViewComponent;
import io.imunity.idp.IdPClientData;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import javax.annotation.security.PermitAll;
import java.io.ByteArrayInputStream;
import java.util.List;

@PermitAll
@Route(value = "/trusted-applications", layout = HomeUiMenu.class)
public class TrustedApplicationsView extends HomeViewComponent
{
	private final TrustedApplicationsController controller;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	TrustedApplicationsView(TrustedApplicationsController controller, MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.controller = controller;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event)
	{
		refresh();
	}

	private void refresh()
	{
		getContent().removeAll();
		List<IdPClientData> applications;
		try
		{
			applications = controller.getApplications();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			return;
		}
		Accordion accordion = new Accordion();
		accordion.setWidthFull();
		accordion.close();
		controller.filterAllowedApplications(applications).forEach(app -> accordion.add(createPanel(app)));
		VerticalLayout mainLayout = new VerticalLayout();
		H2 title = new H2(msg.getMessage("UserHomeUI.trustedApplications"));
		title.getStyle().set("margin", "0");
		mainLayout.add(title);
		mainLayout.add(accordion);
		getContent().add(mainLayout);
	}

	private AccordionPanel createPanel(IdPClientData application)
	{
		AccordionPanel accordionPanel = new AccordionPanel();

		HorizontalLayout summaryLayout = new HorizontalLayout();
		HorizontalLayout titleLayout = new HorizontalLayout();
		titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		if (application.logo.isPresent())
		{
			Image logoI = new Image(new StreamResource("logo", () -> new ByteArrayInputStream(application.logo.get())), "");
			logoI.setHeight(35, Unit.PIXELS);
			titleLayout.add(logoI);
		}

		H3 appName = new H3(application.applicationName.orElse(msg.getMessage("TrustedApplications.unknownApplication")));
		appName.getStyle().set("margin", "0");
		titleLayout.add(appName);

		Button action = new Button(application.accessStatus.equals(IdPClientData.AccessStatus.disallowWithoutAsking)
				? msg.getMessage("TrustedApplications.unblock")
				: msg.getMessage("TrustedApplications.revoke"));
		action.addClickListener(e ->
		{
			if (application.accessStatus.equals(IdPClientData.AccessStatus.disallowWithoutAsking))
			{
				unblockAccess(application);
			} else
			{
				revokeAccess(application);
			}
		});
		summaryLayout.add(titleLayout, action);
		summaryLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		summaryLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		accordionPanel.setSummary(summaryLayout);
		accordionPanel.setContent(getContent(application));

		return accordionPanel;
	}

	private FormLayout getContent(IdPClientData application)
	{
		FormLayout content = new FormLayout();
		content.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		content.getStyle().set("margin-left", "2em");

		application.applicationDomain.ifPresent(s -> content.addFormItem(new Label(s), msg.getMessage("TrustedApplications.applicationDomain")));

		if (application.accessScopes.isPresent() && !application.accessScopes.get().isEmpty())
		{
			Label label = new Label(String.join(", ", application.accessScopes.get()));
			content.addFormItem(label, msg.getMessage("TrustedApplications.accessGrantedTo"));
		}

		application.accessGrantTime.ifPresent(instant -> content.addFormItem(new Label(TimeUtil.formatStandardInstant(instant)), msg.getMessage("TrustedApplications.accessGrantedOn")));

		application.lastAccessTime.ifPresent(instant -> content.addFormItem(new Label(TimeUtil.formatStandardInstant(instant)), msg.getMessage("TrustedApplications.lastAccessTime")));

		if (!application.accessStatus.equals(IdPClientData.AccessStatus.allow))
		{
			Label label = new Label(application.accessStatus.equals(IdPClientData.AccessStatus.allowWithoutAsking)
					? msg.getMessage("TrustedApplications.consentSettingAllow")
					: msg.getMessage("TrustedApplications.consentSettingDisallow"));
			content.addFormItem(label, msg.getMessage("TrustedApplications.consentSettings"));
		}

		VerticalLayout technicalInfoWrapper = new VerticalLayout();
		FormLayout technicalInfoContent = new FormLayout();
		technicalInfoContent.setVisible(false);
		technicalInfoWrapper.setPadding(false);
		technicalInfoWrapper.setAlignItems(FlexComponent.Alignment.END);
		content.add(technicalInfoWrapper);

		LinkButton showTechnicalInfoButton = new LinkButton(msg.getMessage("TrustedApplications.showTechnicalInformation"), e -> {
			{
				boolean vis = technicalInfoContent.isVisible();
				((LinkButton)e.getSource()).setLabel(vis
						? msg.getMessage("TrustedApplications.showTechnicalInformation")
						: msg.getMessage("TrustedApplications.hideTechnicalInformation"));
				technicalInfoContent.setVisible(!vis);
			}
		});

		technicalInfoWrapper.add(showTechnicalInfoButton);
		technicalInfoWrapper.add(technicalInfoContent);

		technicalInfoContent.addFormItem(new Label(application.accessProtocol.toString()), msg.getMessage("TrustedApplications.accessProtocol"));
		technicalInfoContent.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		technicalInfoContent.setSizeUndefined();
		technicalInfoContent.getStyle().set("align-self", "end");
		technicalInfoContent.getStyle().set("box-shadow", "0 0 5px 2px grey");
		application.technicalInformations.forEach(es ->
				technicalInfoContent.addFormItem(new Html("<div>" + es.value + "</div>"), es.titleKey));
		return content;
	}

	private void unblockAccess(IdPClientData application)
	{
		try
		{
			controller.ublockAccess(application.applicationId, application.accessProtocol);
			refresh();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
		}
	}

	private void revokeAccess(IdPClientData application)
	{
		ConfirmDialog confirmDialog = new ConfirmDialog();
		confirmDialog.setCancelable(true);
		confirmDialog.setHeader(msg.getMessage("TrustedApplications.confirmRevokeTitle"));
		confirmDialog.setText(msg.getMessage("TrustedApplications.confirmRevoke", application.applicationName.orElse(msg.getMessage("TrustedApplications.thisApplication"))));
		confirmDialog.addConfirmListener(e -> {
			try
			{
				controller.revokeAccess(application.applicationId, application.accessProtocol);
				refresh();
			} catch (ControllerException ex)
			{
				notificationPresenter.showError(msg.getMessage("error"), ex.getMessage());
			}
		});
		confirmDialog.open();
	}

}
