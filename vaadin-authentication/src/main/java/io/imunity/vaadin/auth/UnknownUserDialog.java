/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.auth.sandbox.SandboxWizardDialog;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.AssociationAccountWizardProvider;
import io.imunity.vaadin.endpoint.common.api.RegistrationFormDialogProvider;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;

/**
 * Dialog presented to users who were correctly authenticated with a remote IdP
 * but are unknown locally.
 * <p>
 * Can show (subject to configuration) two alternatives: either to fill a registration form
 * or to associate the new account with an existing one.
 * <p>
 * This dialog shall be shown only if at least one of the alternatives is enabled in configuration.
 */
class UnknownUserDialog extends Dialog
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UnknownUserDialog.class);
	
	private final MessageSource msg;
	private final UnknownRemotePrincipalResult authNResult;
	private final RegistrationFormDialogProvider formLauncher;
	private final NotificationPresenter notificationPresenter;
	private final AssociationAccountWizardProvider associationAccountWizardProvider;

	UnknownUserDialog(MessageSource msg, UnknownRemotePrincipalResult authNResult,
	                         RegistrationFormDialogProvider formLauncher,
	                         NotificationPresenter notificationPresenter, AssociationAccountWizardProvider associationAccountWizardProvider)
	{
		this.authNResult = authNResult;
		this.formLauncher = formLauncher;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
		this.associationAccountWizardProvider = associationAccountWizardProvider;
		init();
	}

	private void init()
	{
		setHeaderTitle(msg.getMessage("UnknownUserDialog.caption"));
		getFooter().add(new Button(msg.getMessage("cancel"), e -> close()));

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		
		Label mainInfo = new Label(msg.getMessage("UnknownUserDialog.generalInfo"));
		
		HorizontalLayout options = new HorizontalLayout();
		options.setSizeFull();
		int enabledColumns = 0;
		if (authNResult.formForUnknownPrincipal != null)
		{
			log.debug("Adding registration component");
			options.add(getRegistrationComponent());
			enabledColumns++;
		}
		if (authNResult.enableAssociation)
		{
			options.add(getAssociationComponent());
			enabledColumns++;
		}
		setWidth(enabledColumns == 2 ? "var(--big-dialog-width)" : "var(--medium-dialog-width)");

		main.add(mainInfo, options);
		add(main);
	}

	private Component getRegistrationComponent()
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		ret.setPadding(false);
		Label label = new Label(msg.getMessage("UnknownUserDialog.registerInfo"));
		Button register = new Button(msg.getMessage("UnknownUserDialog.register"));
		register.setId("UnknownUserDialog.register");
		register.addThemeVariants(ButtonVariant.LUMO_LARGE);
		register.addClickListener(e ->
				showRegistration(
						authNResult.formForUnknownPrincipal,
						authNResult.getRemotelyAuthenticatedPrincipal())
		);
		ret.add(register, label);
		ret.setAlignItems(FlexComponent.Alignment.CENTER);
		return ret;
	}
	
	private Component getAssociationComponent()
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		ret.setPadding(false);
		Label label = new Label(msg.getMessage("UnknownUserDialog.associationInfo"));
		Button associate = new Button(msg.getMessage("UnknownUserDialog.associate"));
		associate.setId("UnknownUserDialog.associate");
		associate.addThemeVariants(ButtonVariant.LUMO_LARGE);
		associate.addClickListener(e ->
				showAssociation()
		);
		ret.add(associate, label);
		ret.setAlignItems(FlexComponent.Alignment.CENTER);
		return ret;
	}

	protected void showAssociation()
	{
		SandboxWizardDialog dialog = new SandboxWizardDialog();
		Component wizard = associationAccountWizardProvider.getWizardForConnectIdAtLogin(authNResult.getRemotelyAuthenticatedPrincipal(), dialog::close);
		dialog.setHeaderTitle(msg.getMessage("ConnectId.wizardCaption"));
		dialog.add(wizard);
		dialog.open();
		close();
	}
	
	protected void showRegistration(String form, RemotelyAuthenticatedPrincipal ctx)
	{
		try
		{
			formLauncher.showRegistrationDialog(form, ctx, TriggeringMode.afterRemoteLoginWhenUnknownUser,
					this::handleRegistrationError);
			close();
		} catch (Exception e)
		{
			log.error("Can't show a registration form for the remotely authenticated user as configured. " +
					"Probably the form name is wrong.", e);
			notificationPresenter.showError(msg.getMessage("AuthenticationUI.authnErrorTitle"),
					msg.getMessage("AuthenticationUI.problemWithRegistration"));
		}
	}
	
	private void handleRegistrationError(Exception error)
	{
		log.info("Can't show a registration form for the remotely authenticated user - "
				+ "user does not meet form requirements.", error);
		notificationPresenter.showError(msg.getMessage("AuthenticationUI.authnErrorTitle"),
				msg.getMessage("AuthenticationUI.infufficientRegistrationInput"));
	}
}
