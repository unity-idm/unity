/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.imunity.console.views.DirectoryBrowserView;
import io.imunity.console.views.ServicesEditView;
import io.imunity.console.views.ServicesView;
import io.imunity.console.views.authentication.CredentialRequirementsView;
import io.imunity.console.views.authentication.CredentialsView;
import io.imunity.console.views.authentication.FacilitiesView;
import io.imunity.console.views.authentication.RemoteDataProfilesView;
import io.imunity.console.views.authentication.realms.RealmEditView;
import io.imunity.console.views.authentication.realms.RealmsView;
import io.imunity.console.views.maintenance.AboutView;
import io.imunity.console.views.maintenance.audit_log.AuditLogView;
import io.imunity.console.views.maintenance.backup_and_restore.BackupAndRestoreView;
import io.imunity.console.views.maintenance.idp_usage_statistics.IdPUsageStatisticsView;
import io.imunity.console.views.settings.message_templates.MessageTemplateEditView;
import io.imunity.console.views.settings.message_templates.MessageTemplatesView;
import io.imunity.console.views.settings.pki.PKIEditView;
import io.imunity.console.views.settings.pki.PKIView;
import io.imunity.console.views.settings.policy_documents.PolicyDocumentEditView;
import io.imunity.console.views.settings.policy_documents.PolicyDocumentsView;
import io.imunity.vaadin.elements.MenuComponent;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.layout.UnityAppLayout;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ConsoleMenu extends UnityAppLayout
{

	@Autowired
	public ConsoleMenu(VaddinWebLogoutHandler standardWebLogoutHandler, MessageSource msg)
	{
		super(Stream.of(
						MenuComponent.builder(DirectoryBrowserView.class)
								.tabName(msg.getMessage("WebConsoleMenu.directoryBrowser"))
								.icon(VaadinIcon.GROUP)
								.build(),
						MenuComponent.builder(
								MenuComponent.builder(FacilitiesView.class)
										.tabName(msg.getMessage("WebConsoleMenu.authentication.facilities"))
										.icon(VaadinIcon.SIGN_IN)
										.build(),
								MenuComponent.builder(CredentialsView.class)
										.tabName(msg.getMessage("WebConsoleMenu.authentication.localCredentials"))
										.icon(VaadinIcon.LOCK)
										.build(),
								MenuComponent.builder(CredentialRequirementsView.class)
										.tabName(msg.getMessage("WebConsoleMenu.authentication.credentialRequirements"))
										.icon(VaadinIcon.OPTION_A)
										.build(),
								MenuComponent.builder(RealmsView.class)
										.tabName(msg.getMessage("WebConsoleMenu.authentication.realms"))
										.icon(VaadinIcon.GRID_BIG)
										.subViews(RealmEditView.class)
										.build(),
								MenuComponent.builder(RemoteDataProfilesView.class)
										.tabName(msg.getMessage("WebConsoleMenu.authentication.inputTranslation"))
										.icon(VaadinIcon.DOWNLOAD)
										.build()
								)
								.tabName(msg.getMessage("WebConsoleMenu.authentication"))
								.icon(VaadinIcon.KEY_O)
								.build(),
						MenuComponent.builder(ServicesView.class)
								.tabName(msg.getMessage("WebConsoleMenu.services"))
								.icon(VaadinIcon.SERVER)
								.subViews(ServicesEditView.class)
								.build(),
						MenuComponent.builder(
								MenuComponent.builder(MessageTemplatesView.class)
										.tabName(msg.getMessage("WebConsoleMenu.settings.messageTemplates"))
										.icon(VaadinIcon.ENVELOPES_O)
										.subViews(MessageTemplateEditView.class)
										.build(),
								MenuComponent.builder(PolicyDocumentsView.class)
										.tabName(msg.getMessage("WebConsoleMenu.settings.policyDocuments"))
										.icon(VaadinIcon.CHECK_SQUARE_O)
										.subViews(PolicyDocumentEditView.class)
										.build(),
								MenuComponent.builder(PKIView.class)
										.tabName(msg.getMessage("WebConsoleMenu.settings.publicKeyInfrastructure"))
										.icon(VaadinIcon.DIPLOMA)
										.subViews(PKIEditView.class)
										.build()
								)
								.tabName(msg.getMessage("WebConsoleMenu.settings"))
								.icon(VaadinIcon.COGS)
								.build(),
						MenuComponent.builder(
								MenuComponent.builder(AuditLogView.class)
										.tabName(msg.getMessage("WebConsoleMenu.maintenance.auditLog"))
										.icon(VaadinIcon.TABS)
										.build(),
								MenuComponent.builder(BackupAndRestoreView.class)
										.tabName(msg.getMessage("WebConsoleMenu.maintenance.backupAndRestore"))
										.icon(VaadinIcon.CLOUD_DOWNLOAD_O)
										.build(),
								MenuComponent.builder(IdPUsageStatisticsView.class)
										.tabName(msg.getMessage("WebConsoleMenu.maintenance.idpStatistics"))
										.icon(VaadinIcon.TABS)
										.build(),
								MenuComponent.builder(AboutView.class)
										.tabName(msg.getMessage("WebConsoleMenu.maintenance.about"))
										.icon(VaadinIcon.INFO)
										.build()
								)
								.tabName(msg.getMessage("WebConsoleMenu.maintenance"))
								.icon(VaadinIcon.TOOLS)
								.build()
						)
						.collect(toList()), standardWebLogoutHandler, msg, List.of()
		);
		super.initView();

		Image image = createDefaultImage();
		addToLeftContainerAsFirst(createImageLayout(image));
		activateLeftContainerMinimization(image);
	}

	private HorizontalLayout createImageLayout(Component image)
	{
		HorizontalLayout imageLayout = new HorizontalLayout();
		imageLayout.getStyle().set("margin-top", "var(--medium-margin)");
		imageLayout.getStyle().set("margin-bottom", "var(--medium-margin)");
		imageLayout.setJustifyContentMode(JustifyContentMode.CENTER);
		imageLayout.add(image);
		return imageLayout;
	}

	private static Image createDefaultImage()
	{
		Image tmpImage = new Image("../unitygw/img/other/logo-hand.png", "");
		tmpImage.setId("unity-logo-image");
		return tmpImage;
	}

	@Override
	public void showRouterLayoutContent(HasElement content)
	{
		super.showRouterLayoutContent(content);
	}
}