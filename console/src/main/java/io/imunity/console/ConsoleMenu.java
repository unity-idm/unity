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
import io.imunity.console.views.authentication.credential_requirements.CredentialRequirementsEditView;
import io.imunity.console.views.authentication.credential_requirements.CredentialRequirementsView;
import io.imunity.console.views.authentication.credentials.CredentialsEditView;
import io.imunity.console.views.authentication.credentials.CredentialsInfoView;
import io.imunity.console.views.authentication.credentials.CredentialsView;
import io.imunity.console.views.authentication.facilities.AuthenticationFlowEditView;
import io.imunity.console.views.authentication.facilities.AuthenticatorEditView;
import io.imunity.console.views.authentication.facilities.FacilitiesView;
import io.imunity.console.views.authentication.input_profiles.EditInputTranslationView;
import io.imunity.console.views.authentication.input_profiles.NewInputTranslationView;
import io.imunity.console.views.authentication.input_profiles.RemoteDataProfilesView;
import io.imunity.console.views.authentication.realms.RealmEditView;
import io.imunity.console.views.authentication.realms.RealmsView;
import io.imunity.console.views.directory_browser.DirectoryBrowserView;
import io.imunity.console.views.directory_setup.attribute_classes.AttributeClassesEditView;
import io.imunity.console.views.directory_setup.attribute_classes.AttributeClassesView;
import io.imunity.console.views.directory_setup.attribute_types.AttributeTypesView;
import io.imunity.console.views.directory_setup.automation.AutomationEditView;
import io.imunity.console.views.directory_setup.automation.AutomationRunView;
import io.imunity.console.views.directory_setup.automation.AutomationView;
import io.imunity.console.views.directory_setup.identity_types.IdentityTypesView;
import io.imunity.console.views.identity_provider.endpoints.AdditionalIdpServiceView;
import io.imunity.console.views.identity_provider.endpoints.EditIdpServiceView;
import io.imunity.console.views.identity_provider.endpoints.IdpServicesView;
import io.imunity.console.views.identity_provider.endpoints.NewIdpServiceView;
import io.imunity.console.views.identity_provider.released_profile.EditOutputTranslationView;
import io.imunity.console.views.identity_provider.released_profile.NewOutputTranslationView;
import io.imunity.console.views.identity_provider.released_profile.ReleasedDataProfilesView;
import io.imunity.console.views.maintenance.AboutView;
import io.imunity.console.views.maintenance.audit_log.AuditLogView;
import io.imunity.console.views.maintenance.backup_and_restore.BackupAndRestoreView;
import io.imunity.console.views.maintenance.idp_usage_statistics.IdPUsageStatisticsView;
import io.imunity.console.views.services.EditServiceView;
import io.imunity.console.views.services.NewServiceView;
import io.imunity.console.views.services.ServicesView;
import io.imunity.console.views.settings.message_templates.MessageTemplateEditView;
import io.imunity.console.views.settings.message_templates.MessageTemplatesView;
import io.imunity.console.views.settings.pki.PKIEditView;
import io.imunity.console.views.settings.pki.PKIView;
import io.imunity.console.views.settings.policy_documents.PolicyDocumentEditView;
import io.imunity.console.views.settings.policy_documents.PolicyDocumentsView;
import io.imunity.console.views.signup_and_enquiry.forms.EnquiryView;
import io.imunity.console.views.signup_and_enquiry.forms.FormsView;
import io.imunity.console.views.signup_and_enquiry.forms.RegistrationView;
import io.imunity.console.views.signup_and_enquiry.invitations.InvitationsView;
import io.imunity.console.views.signup_and_enquiry.requests.RequestsView;
import io.imunity.vaadin.elements.MenuComponent;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.layout.UnityAppLayout;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;

import java.util.List;
import java.util.stream.Stream;

import static io.imunity.vaadin.elements.CSSVars.MEDIUM_MARGIN;
import static io.imunity.vaadin.elements.CssClassNames.LOGO_IMAGE;
import static java.util.stream.Collectors.toList;

public class ConsoleMenu extends UnityAppLayout
{

	@Autowired
	public ConsoleMenu(VaddinWebLogoutHandler standardWebLogoutHandler, MessageSource msg, AuthorizationManagement authorizationManagement)
	{
		super(Stream.of(
						MenuComponent.builder(DirectoryBrowserView.class)
								.tabName(msg.getMessage("WebConsoleMenu.directoryBrowser"))
								.icon(VaadinIcon.GROUP)
								.build(),
						MenuComponent.builder(
								MenuComponent.builder(FormsView.class)
										.tabName(msg.getMessage("WebConsoleMenu.signupAndEnquiry.forms"))
										.icon(VaadinIcon.FORM)
										.subViews(RegistrationView.class, EnquiryView.class)
										.build(),
								MenuComponent.builder(RequestsView.class)
										.tabName(msg.getMessage("WebConsoleMenu.signup_and_enquiry.requests"))
										.icon(VaadinIcon.USER_CARD)
										.build(),
								MenuComponent.builder(InvitationsView.class)
										.tabName(msg.getMessage("WebConsoleMenu.signup_and_enquiry.invitations"))
										.icon(VaadinIcon.TAXI)
										.build())
								.tabName(msg.getMessage("WebConsoleMenu.signup_and_enquiry"))
								.icon(VaadinIcon.USER_CHECK)
								.build(),			
						MenuComponent.builder(
								MenuComponent.builder(IdpServicesView.class)
								.tabName(msg.getMessage("WebConsoleMenu.identityProvider.endpoints"))
								.icon(VaadinIcon.GLOBE)
								.subViews(NewIdpServiceView.class, EditIdpServiceView.class, AdditionalIdpServiceView.class)
								.build(),
								MenuComponent.builder(ReleasedDataProfilesView.class)
										.tabName(msg.getMessage("WebConsoleMenu.identityProvider.releasedProfiles"))
										.icon(VaadinIcon.UPLOAD)
										.subViews(NewOutputTranslationView.class, EditOutputTranslationView.class)
										.build())
								.tabName(msg.getMessage("WebConsoleMenu.identityProvider"))
								.icon(VaadinIcon.GLOBE_WIRE)
								.build(),
						MenuComponent.builder(
								MenuComponent.builder(FacilitiesView.class)
										.tabName(msg.getMessage("WebConsoleMenu.authentication.facilities"))
										.icon(VaadinIcon.SIGN_IN)
										.subViews(AuthenticationFlowEditView.class, AuthenticatorEditView.class)
										.build(),
								MenuComponent.builder(CredentialsView.class)
										.tabName(msg.getMessage("WebConsoleMenu.authentication.localCredentials"))
										.icon(VaadinIcon.LOCK)
										.subViews(CredentialsEditView.class, CredentialsInfoView.class)
										.build(),
								MenuComponent.builder(CredentialRequirementsView.class)
										.tabName(msg.getMessage("WebConsoleMenu.authentication.credentialRequirements"))
										.icon(VaadinIcon.OPTION_A)
										.subViews(CredentialRequirementsEditView.class)
										.build(),
								MenuComponent.builder(RealmsView.class)
										.tabName(msg.getMessage("WebConsoleMenu.authentication.realms"))
										.icon(VaadinIcon.GRID_BIG)
										.subViews(RealmEditView.class)
										.build(),
								MenuComponent.builder(RemoteDataProfilesView.class)
										.tabName(msg.getMessage("WebConsoleMenu.authentication.inputTranslation"))
										.icon(VaadinIcon.DOWNLOAD)
										.subViews(NewInputTranslationView.class, EditInputTranslationView.class)
										.build()
								)
								.tabName(msg.getMessage("WebConsoleMenu.authentication"))
								.icon(VaadinIcon.KEY_O)
								.build(),
						MenuComponent.builder(ServicesView.class)
								.tabName(msg.getMessage("WebConsoleMenu.services"))
								.icon(VaadinIcon.SERVER)
								.subViews(NewServiceView.class, EditServiceView.class)
								.build(),
						MenuComponent.builder(
								MenuComponent.builder(AttributeTypesView.class)
										.tabName(msg.getMessage("WebConsoleMenu.directorySetup.attributeTypes"))
										.icon(VaadinIcon.TAGS)
										.build(),
								MenuComponent.builder(IdentityTypesView.class)
										.tabName(msg.getMessage("WebConsoleMenu.directorySetup.identityTypes"))
										.icon(VaadinIcon.CLIPBOARD_USER)
										.build(),
								MenuComponent.builder(AttributeClassesView.class)
										.tabName(msg.getMessage("WebConsoleMenu.directorySetup.attributeClasses"))
										.icon(VaadinIcon.ARCHIVES)
										.subViews(AttributeClassesEditView.class)
										.build(),
								MenuComponent.builder(AutomationView.class)
										.tabName(msg.getMessage("WebConsoleMenu.directorySetup.automation"))
										.icon(VaadinIcon.CALENDAR_USER)
										.subViews(AutomationEditView.class, AutomationRunView.class)
										.build()
								)
								.tabName(msg.getMessage("WebConsoleMenu.directorySetup"))
								.icon(VaadinIcon.FILE_TREE)
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

		try
		{
			if(!authorizationManagement.hasAdminAccess())
			{
				showCriticalException(standardWebLogoutHandler, msg);
				return;
			}
		}
		catch (AuthorizationException e)
		{
			showCriticalException(standardWebLogoutHandler, msg);
			return;
		}

		super.initView();

		Image image = createDefaultImage();
		addToLeftContainerAsFirst(createImageLayout(image));
		activateLeftContainerMinimization(image);
	}

	private static void showCriticalException(VaddinWebLogoutHandler standardWebLogoutHandler, MessageSource msg)
	{
		NotificationPresenter.showCriticalError(standardWebLogoutHandler::logout, msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
	}

	private HorizontalLayout createImageLayout(Component image)
	{
		HorizontalLayout imageLayout = new HorizontalLayout();
		imageLayout.getStyle().set("margin-top", MEDIUM_MARGIN.value());
		imageLayout.getStyle().set("margin-bottom", MEDIUM_MARGIN.value());
		imageLayout.setJustifyContentMode(JustifyContentMode.CENTER);
		imageLayout.add(image);
		return imageLayout;
	}

	private static Image createDefaultImage()
	{
		Image tmpImage = new Image("../unitygw/img/other/logo-hand.png", "");
		tmpImage.setClassName(LOGO_IMAGE.getName());
		return tmpImage;
	}

	@Override
	public void showRouterLayoutContent(HasElement content)
	{
		super.showRouterLayoutContent(content);
	}
}
