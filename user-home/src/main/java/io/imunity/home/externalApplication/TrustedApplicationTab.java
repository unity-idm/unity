/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.externalApplication;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.home.TrustedApplicationData;
import pl.edu.icm.unity.engine.api.home.TrustedApplicationData.AccessStatus;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@PrototypeComponent
public class TrustedApplicationTab extends CustomComponent
{
	private final TrustedApplicationsController controller;
	private final MessageSource msg;
	private VerticalLayout main;

	@Autowired
	TrustedApplicationTab(TrustedApplicationsController controller, MessageSource msg)
	{
		this.controller = controller;
		this.msg = msg;
		init();
	}

	private void init()
	{
		main = new VerticalLayout();
		setCompositionRoot(main);
		refresh();
	}

	private void refresh()
	{
		main.removeAllComponents();
		List<TrustedApplicationData> applications;
		try
		{
			applications = controller.getApplications();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}
		List<TrustedApplicationData> allowedApp = applications.stream()
				.filter(a -> a.accessStatus.equals(AccessStatus.allowWithoutAsking)
						|| a.accessStatus.equals(AccessStatus.allow))
				.collect(Collectors.toList());
		if (allowedApp.size() > 0)
		{
			Label appWithAccess = new Label();
			appWithAccess.setValue(msg.getMessage("TrustedApplications.applicationsWithAccess"));
			appWithAccess.setStyleName(Styles.textXLarge.toString());
			appWithAccess.addStyleName(Styles.bold.toString());
			main.addComponent(appWithAccess);
			allowedApp.forEach(a -> main.addComponent(new TrustedApplicationComponent(a)));
		}
		List<TrustedApplicationData> disallowedApp = applications.stream()
				.filter(a -> a.accessStatus.equals(AccessStatus.disallowWithoutAsking)).collect(Collectors.toList());
		if (disallowedApp.size() > 0)
		{
			Label appWithDenied = new Label();
			appWithDenied.setValue(msg.getMessage("TrustedApplications.applicationsWithDenied"));
			appWithDenied.setStyleName(Styles.textXLarge.toString());
			appWithDenied.addStyleName(Styles.bold.toString());
			main.addComponent(appWithDenied);
			disallowedApp.forEach(a -> main.addComponent(new TrustedApplicationComponent(a)));
		}
	}

	public class TrustedApplicationComponent extends CustomComponent
	{
		private Button showHide;
		private FormLayout content;

		public TrustedApplicationComponent(TrustedApplicationData application)
		{
			VerticalLayout header = initHeader(application);
			content = initContent(application);
			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);
			showHideContent(false);
			main.addComponent(header);
			main.addComponent(content);
			main.addComponent(HtmlTag.horizontalLine());
			setCompositionRoot(main);
		}

		private FormLayout initContent(TrustedApplicationData application)
		{

			FormLayoutWithFixedCaptionWidth content = new FormLayoutWithFixedCaptionWidth();
			if (application.applicationDomain.isPresent())
			{
				Label appDomain = new Label();
				appDomain.setCaption(msg.getMessage("TrustedApplications.applicationDomain"));
				appDomain.setValue(application.applicationDomain.get());
				content.addComponent(appDomain);
			}

			if (application.accessScopes.isPresent() && !application.accessScopes.get().isEmpty())
			{
				Label appDomain = new Label();
				appDomain.setCaption(msg.getMessage("TrustedApplications.accessGrantedTo"));
				appDomain.setValue(String.join(", ", application.accessScopes.get()));
				content.addComponent(appDomain);
			}

			if (application.accessGrantTime.isPresent())
			{
				Label appDomain = new Label();
				appDomain.setCaption(msg.getMessage("TrustedApplications.accessGrantedOn"));
				appDomain.setValue(TimeUtil.formatStandardInstant(application.accessGrantTime.get()));
				content.addComponent(appDomain);
			}

			if (application.lastAccessTime.isPresent())
			{
				Label appDomain = new Label();
				appDomain.setCaption(msg.getMessage("TrustedApplications.lastAccessTime"));
				appDomain.setValue(TimeUtil.formatStandardInstant(application.lastAccessTime.get()));
				content.addComponent(appDomain);
			}

			if (!application.accessStatus.equals(AccessStatus.allow))
			{
				Label consentSettings = new Label();
				consentSettings.setCaption(msg.getMessage("TrustedApplications.consentSettings"));
				consentSettings.setValue(application.accessStatus.equals(AccessStatus.allowWithoutAsking)
						? msg.getMessage("TrustedApplications.consentSettingAllow")
						: msg.getMessage("TrustedApplications.consentSettingDisallow"));
				content.addComponent(consentSettings);
			}

			VerticalLayout technicalInfoWrapper = new VerticalLayout();
			FormLayoutWithFixedCaptionWidth technicalInfoContent = new FormLayoutWithFixedCaptionWidth();
			technicalInfoWrapper.setMargin(false);
			technicalInfoContent.setMargin(false);
			technicalInfoContent.setVisible(false);
			content.addComponent(technicalInfoWrapper);

			Button showTechnicalInfo = new Button();
			showTechnicalInfo.setCaption(msg.getMessage("TrustedApplications.showTechnicalInformation"));
			showTechnicalInfo.addStyleName(Styles.vButtonLink.toString());
			technicalInfoWrapper.addComponent(showTechnicalInfo);
			technicalInfoWrapper.addComponent(technicalInfoContent);

			Label protocol = new Label();
			protocol.setCaption(msg.getMessage("TrustedApplications.accessProtocol"));
			protocol.setValue(application.accessProtocol.toString());
			protocol.setStyleName(Styles.fontMonospace.toString());
			technicalInfoContent.addComponent(protocol);

			application.technicalInformations.forEach(es ->
			{
				Label i = new Label();
				i.setCaption(es.titleKey);
				i.setValue(es.value);
				i.setStyleName(Styles.fontMonospace.toString());
				technicalInfoContent.addComponent(i);
			});

			showTechnicalInfo.addClickListener(e ->
			{
				showTechnicalInfo.setCaption(technicalInfoContent.isVisible()
						? msg.getMessage("TrustedApplications.showTechnicalInformation")
						: msg.getMessage("TrustedApplications.hideTechnicalInformation"));
				technicalInfoContent.setVisible(!technicalInfoContent.isVisible());
			});
			content.setMargin(false);
			content.setSpacing(true);

			return content;
		}

		private VerticalLayout initHeader(TrustedApplicationData application)
		{
			VerticalLayout headerWrapper = new VerticalLayout();
			headerWrapper.setMargin(false);
			headerWrapper.setSpacing(false);

			HorizontalLayout header = new HorizontalLayout();
			header.setSizeFull();
			header.setMargin(false);

			showHide = new Button(Images.downArrow.getResource());
			showHide.addStyleName(Styles.vButtonLink.toString());
			showHide.addStyleName(Styles.toolbarButton.toString());
			showHide.addStyleName(Styles.vButtonBorderless.toString());
			showHide.addClickListener(event -> showHideContent(!content.isVisible()));
			header.addComponent(showHide);
			header.setComponentAlignment(showHide, Alignment.MIDDLE_LEFT);
			header.setExpandRatio(showHide, 0);

			if (application.logo.isPresent())
			{
				Resource clientLogo = new StreamResource(() -> new ByteArrayInputStream(application.logo.get()),
						"logo");
				Image logoI = new Image();
				logoI.setHeight(35, Unit.PIXELS);
				logoI.setSource(clientLogo);

				header.addComponent(logoI);
				header.setComponentAlignment(logoI, Alignment.MIDDLE_LEFT);
				header.setExpandRatio(logoI, 0);
			}

			Label appName = new Label(application.applicationName);
			appName.setStyleName(Styles.textLarge.toString());
			appName.addStyleName(Styles.bold.toString());
			header.addComponent(appName);
			header.setComponentAlignment(appName, Alignment.MIDDLE_LEFT);
			header.setExpandRatio(appName, 10);

			Button action = new Button();
			action.setCaption(application.accessStatus.equals(AccessStatus.disallowWithoutAsking)
					? msg.getMessage("TrustedApplications.unblock")
					: msg.getMessage("TrustedApplications.revoke"));
			action.addClickListener(e ->
			{
				if (application.accessStatus.equals(AccessStatus.disallowWithoutAsking))
				{
					unblockAccess(application);
				} else
				{
					revokeAccess(application);
				}
			});
			header.addComponent(action);
			header.setComponentAlignment(action, Alignment.BOTTOM_RIGHT);

			headerWrapper.addComponent(header);
			return headerWrapper;
		}

		private void unblockAccess(TrustedApplicationData application)
		{
			try
			{
				controller.ublockAccess(application.applicationId, application.accessProtocol);
				refresh();
			} catch (ControllerException e)
			{
				NotificationPopup.showError(e);
			}
		}

		private void revokeAccess(TrustedApplicationData application)
		{
			new ConfirmDialog(msg, msg.getMessage("TrustedApplications.confirmRevokeTitle"),
					msg.getMessage("TrustedApplications.confirmRevoke", application.applicationName), () ->
					{
						try
						{
							controller.revokeAccess(application.applicationId, application.accessProtocol);
							refresh();
						} catch (ControllerException e)
						{
							NotificationPopup.showError(e);

						}

					}).show();
		}

		private void showHideContent(boolean show)
		{
			showHide.setIcon(show ? Images.upArrow.getResource() : Images.downArrow.getResource());
			content.setVisible(show);
		}
	}

}