/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.remote;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.webui.association.atlogin.ConnectIdAtLoginWizardProvider;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.forms.reg.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.webui.sandbox.wizard.SandboxWizardDialog;

/**
 * Dialog presented to users who were correctly authenticated with a remote IdP
 * but are unknown locally.
 * <p>
 * Can show (subject to configuration) two alternatives: either to fill a registration form
 * or to associate the new account with an existing one.
 * <p>
 * This dialog shall be shown only if at least one of the alternatives is enabled in configuration.
 * @author K. Benedyczak
 */
public class UnknownUserDialog extends AbstractDialog
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UnknownUserDialog.class);
	
	private AuthenticationResult authNResult;
	private InsecureRegistrationFormLauncher formLauncher;

	private SandboxAuthnNotifier sandboxAuthnNotifier;
	private InputTranslationEngine inputTranslationEngine;

	private String sandboxURL;

	public UnknownUserDialog(UnityMessageSource msg, AuthenticationResult authNResult,
			InsecureRegistrationFormLauncher formLauncher, SandboxAuthnNotifier sandboxAuthnNotifier,
			InputTranslationEngine inputTranslationEngine, String sandboxURL)
	{
		super(msg, msg.getMessage("UnknownUserDialog.caption"), msg.getMessage("cancel"));
		setSizeEm(50, 25);
		this.authNResult = authNResult;
		this.formLauncher = formLauncher;
		this.sandboxAuthnNotifier = sandboxAuthnNotifier;
		this.inputTranslationEngine = inputTranslationEngine;
		this.sandboxURL = sandboxURL;
	}

	@Override
	protected Component getContents() throws Exception
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		
		Label mainInfo = new Label(msg.getMessage("UnknownUserDialog.generalInfo"));
		
		HorizontalLayout options = new HorizontalLayout();
		options.setSizeFull();
		if (authNResult.getFormForUnknownPrincipal() != null)
		{
			log.debug("Adding registration component");
			options.addComponent(getRegistrationComponent());
		}
		if (authNResult.isEnableAssociation())
		{
			options.addComponent(getAssociationComponent());
		}
		
		main.addComponents(mainInfo, options);
		return main;
	}

	private Component getRegistrationComponent()
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		Label label = new Label(msg.getMessage("UnknownUserDialog.registerInfo"));
		label.setSizeFull();
		Button register = new Button(msg.getMessage("UnknownUserDialog.register"));
		register.setId("UnknownUserDialog.register");
		register.addStyleName(ValoTheme.BUTTON_LARGE);
		register.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				showRegistration(authNResult.getFormForUnknownPrincipal(), 
						authNResult.getRemoteAuthnContext());
			}
		});
		ret.addComponents(register, label);
		ret.setComponentAlignment(label, Alignment.MIDDLE_CENTER);
		ret.setComponentAlignment(register, Alignment.MIDDLE_CENTER);
		return ret;
	}
	
	private Component getAssociationComponent()
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		Label label = new Label(msg.getMessage("UnknownUserDialog.associationInfo"));
		label.setSizeFull();
		Button associate = new Button(msg.getMessage("UnknownUserDialog.associate"));
		associate.setId("UnknownUserDialog.associate");
		associate.addStyleName(ValoTheme.BUTTON_LARGE);
		associate.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				showAssociation();
			}
		});
		ret.addComponents(associate, label);
		ret.setComponentAlignment(label, Alignment.MIDDLE_CENTER);
		ret.setComponentAlignment(associate, Alignment.MIDDLE_CENTER);
		return ret;
	}

	protected void showAssociation()
	{
		
		ConnectIdAtLoginWizardProvider wizardProv = new ConnectIdAtLoginWizardProvider(msg, 
				sandboxURL, sandboxAuthnNotifier, inputTranslationEngine, 
				authNResult.getRemoteAuthnContext());
		SandboxWizardDialog dialog = new SandboxWizardDialog(wizardProv.getWizardInstance(), 
				wizardProv.getCaption());
		dialog.show();
		close();
	}
	
	protected void showRegistration(String form, RemotelyAuthenticatedContext ctx)
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
			NotificationPopup.showError(msg.getMessage("AuthenticationUI.authnErrorTitle"), 
					msg.getMessage("AuthenticationUI.problemWithRegistration"));
		}
	}
	
	private void handleRegistrationError(Exception error)
	{
		log.debug("Can't show a registration form for the remotely authenticated user - "
				+ "user does not meet form requirements.", error);
		NotificationPopup.showError(msg.getMessage("AuthenticationUI.authnErrorTitle"), 
				msg.getMessage("AuthenticationUI.infufficientRegistrationInput"));
	}
	
	@Override
	protected void onConfirm()
	{
		//do nothing - confirm == cancel, i.e. simply close.
		onCancel();
	}
}
