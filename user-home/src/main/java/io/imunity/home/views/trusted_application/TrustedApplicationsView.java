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
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.imunity.home.views.HomeUiMenu;
import io.imunity.home.views.HomeViewComponent;
import io.imunity.idp.IdPClientData;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;

import java.io.ByteArrayInputStream;
import java.util.List;

import static io.imunity.vaadin.elements.CSSVars.BIG_MARGIN;

@PermitAll
@Breadcrumb(key = "UserHomeUI.trustedApplications")
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
		
		VerticalLayout mainLayout = new VerticalLayout();
		getContent().add(mainLayout);

		
	
		List<IdPClientData> allowedApplications = controller.filterAllowedApplications(applications);
		H2 title = new H2(msg.getMessage("TrustedApplications.applicationsWithAccess"));
		title.getStyle()
				.set("margin", "0");
		mainLayout.add(title);		
		if (allowedApplications.isEmpty())
		{
			mainLayout.add(new H4(msg.getMessage("TrustedApplications.noneTrustedApplications")));

		} else
		{
			Accordion accordion = new Accordion();
			accordion.setWidthFull();
			accordion.close();
			allowedApplications.forEach(app -> accordion.add(createPanel(app)));
			mainLayout.add(accordion);
		}

		List<IdPClientData> disallowedApplications = controller.filterDisallowedApplications(applications);
		if (!disallowedApplications.isEmpty())
		{
			Accordion uaccordion = new Accordion();
			uaccordion.setWidthFull();
			uaccordion.close();
			disallowedApplications.forEach(app -> uaccordion.add(createPanel(app)));
			H2 utitle = new H2(msg.getMessage("TrustedApplications.applicationsWithDenied"));
			utitle.getStyle()
					.set("margin", "0");
			mainLayout.add(utitle);
			mainLayout.add(uaccordion);
		}

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
		accordionPanel.add(getContent(application));

		return accordionPanel;
	}

	private FormLayout getContent(IdPClientData application)
	{
		FormLayout content = new FormLayout();
		content.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		content.getStyle().set("margin-left", BIG_MARGIN.value());

		application.applicationDomain.ifPresent(s -> content.addFormItem(new Span(s), msg.getMessage("TrustedApplications.applicationDomain")));

		if (application.accessScopes.isPresent() && !application.accessScopes.get().isEmpty())
		{
			Span label = new Span(String.join(", ", application.accessScopes.get()));
			content.addFormItem(label, msg.getMessage("TrustedApplications.accessGrantedTo"));
		}

		application.accessGrantTime
				.ifPresent(instant -> content.addFormItem(new Span(TimeUtil.formatStandardInstant(instant)),
						msg.getMessage("TrustedApplications.accessGrantedOn")));

		application.accessDeniedTime
				.ifPresent(instant -> content.addFormItem(new Span(TimeUtil.formatStandardInstant(instant)),
						msg.getMessage("TrustedApplications.accessDeniedOn")));

		application.lastAccessTime
				.ifPresent(instant -> content.addFormItem(new Span(TimeUtil.formatStandardInstant(instant)),
						msg.getMessage("TrustedApplications.lastAccessTime")));

		if (!application.accessStatus.equals(IdPClientData.AccessStatus.allow))
		{
			Span label = new Span(application.accessStatus.equals(IdPClientData.AccessStatus.allowWithoutAsking)
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

		technicalInfoContent.addFormItem(new Span(application.accessProtocol.toString()), msg.getMessage("TrustedApplications.accessProtocol"));
		technicalInfoContent.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		technicalInfoContent.setSizeUndefined();
		technicalInfoContent.setId("technical-info-content");
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
