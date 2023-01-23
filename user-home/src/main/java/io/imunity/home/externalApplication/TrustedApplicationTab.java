/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.externalApplication;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.idp.IdPClientData;
import io.imunity.idp.IdPClientData.AccessStatus;
import pl.edu.icm.unity.MessageSource;
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

	public void refresh()
	{
		main.removeAllComponents();
		List<IdPClientData> applications;
		try
		{
			applications = controller.getApplications();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}
		List<IdPClientData> allowedApp = controller.filterAllowedApplications(applications);

		Label appWithAccess = new Label();
		appWithAccess.setValue(msg.getMessage("TrustedApplications.applicationsWithAccess"));
		appWithAccess.setStyleName(Styles.textXLarge.toString());
		appWithAccess.addStyleName(Styles.bold.toString());
		main.addComponent(appWithAccess);
		main.addComponent(new Label(""));
		
		
		if (allowedApp.size() > 0)
		{
			allowedApp.forEach(a -> main.addComponent(new TrustedApplicationComponent(a)));
		}else {
			Label noApp = new Label();
			noApp.setValue(msg.getMessage("TrustedApplications.noneTrustedApplications"));
			noApp.setStyleName(Styles.emphasized.toString());
			main.addComponent(noApp);		
		}
		
		
		List<IdPClientData> disallowedApp = controller.filterDisallowedApplications(applications);
		if (disallowedApp.size() > 0)
		{
			Label appWithDenied = new Label();
			appWithDenied.setValue(msg.getMessage("TrustedApplications.applicationsWithDenied"));
			appWithDenied.setStyleName(Styles.textXLarge.toString());
			appWithDenied.addStyleName(Styles.bold.toString());
			main.addComponent(appWithDenied);
			main.addComponent(new Label(""));
			disallowedApp.forEach(a -> main.addComponent(new TrustedApplicationComponent(a)));
		}
	}

	public class TrustedApplicationComponent extends CustomComponent
	{
		private Button showHide;
		private FormLayout content;

		public TrustedApplicationComponent(IdPClientData application)
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

		private FormLayout initContent(IdPClientData application)
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
				Label accessGrantedTo = new Label();
				accessGrantedTo.setCaption(msg.getMessage("TrustedApplications.accessGrantedTo"));
				accessGrantedTo.setValue(String.join(", <br>", application.accessScopes.get()));
				accessGrantedTo.setContentMode(ContentMode.HTML);
				accessGrantedTo.addStyleName(Styles.wordWrap.toString());
				content.addComponent(accessGrantedTo);
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
			technicalInfoWrapper.setMargin(new MarginInfo(true, false));
			technicalInfoContent.setMargin(false);
			technicalInfoContent.setVisible(false);
			content.addComponent(technicalInfoWrapper);

			Button showTechnicalInfoButton = new Button();
			showTechnicalInfoButton.setCaption(msg.getMessage("TrustedApplications.showTechnicalInformation"));
			showTechnicalInfoButton.addStyleName(Styles.vButtonLink.toString());
			technicalInfoWrapper.addComponent(showTechnicalInfoButton);
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
				i.addStyleName(Styles.wordWrap.toString());
				i.setContentMode(ContentMode.HTML);
				technicalInfoContent.addComponent(i);
			});

			showTechnicalInfoButton.addClickListener(e ->
			{
				
				boolean vis = technicalInfoContent.isVisible();
				showTechnicalInfoButton.setCaption(vis
					? msg.getMessage("TrustedApplications.showTechnicalInformation")
						: msg.getMessage("TrustedApplications.hideTechnicalInformation"));
				technicalInfoContent.setVisible(!vis);
			});
			content.setMargin(false);
			content.setSpacing(true);

			return content;
		}

		private VerticalLayout initHeader(IdPClientData application)
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

			Label appName = new Label(application.applicationName.orElse(msg.getMessage("TrustedApplications.unknownApplication")));
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

		private void unblockAccess(IdPClientData application)
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

		private void revokeAccess(IdPClientData application)
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