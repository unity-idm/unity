/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.AuthenticationScreen;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;
import pl.edu.icm.unity.webui.authn.tile.SelectedAuthNPanel;
import pl.edu.icm.unity.webui.authn.tile.SelectedAuthNPanel.AuthenticationListener;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Organizes authentication options in a single column, making them instantly clickable.
 * 
 * @author K. Benedyczak
 */
public class ColumnInstantAuthenticationScreen extends CustomComponent implements AuthenticationScreen
{
	protected final UnityMessageSource msg;
	private final VaadinEndpointProperties config;
	private final ResolvedEndpoint endpointDescription;
	private final Supplier<Boolean> outdatedCredentialDialogLauncher;
	private final Runnable registrationDialogLauncher;
	private final boolean enableRegistration;
	private final CancelHandler cancelHandler;
	
	private final EntityManagement idsMan;
	private final ExecutorsService execService;
	private final Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider;
	private final WebAuthenticationProcessor authnProcessor;	
	private final LocaleChoiceComponent localeChoice;
	protected List<AuthenticationOption> authenticators;
	private SelectedAuthNPanel authNPanelInProgress;
	
	public ColumnInstantAuthenticationScreen(UnityMessageSource msg, VaadinEndpointProperties config,
			ResolvedEndpoint endpointDescription,
			Supplier<Boolean> outdatedCredentialDialogLauncher,
			Runnable registrationDialogLauncher, CancelHandler cancelHandler,
			EntityManagement idsMan,
			ExecutorsService execService, boolean enableRegistration,
			Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider,
			WebAuthenticationProcessor authnProcessor,
			LocaleChoiceComponent localeChoice,
			List<AuthenticationOption> authenticators)
	{
		this.msg = msg;
		this.config = config;
		this.endpointDescription = endpointDescription;
		this.outdatedCredentialDialogLauncher = outdatedCredentialDialogLauncher;
		this.registrationDialogLauncher = registrationDialogLauncher;
		this.cancelHandler = cancelHandler;
		this.idsMan = idsMan;
		this.execService = execService;
		this.enableRegistration = enableRegistration;
		this.unknownUserDialogProvider = unknownUserDialogProvider;
		this.authnProcessor = authnProcessor;
		this.localeChoice = localeChoice;
		this.authenticators = authenticators;
		init();
	}

	@Override
	public void refresh(VaadinRequest request) 
	{
		refreshAuthenticationState(request);
	}
	
	protected void init()
	{
		VerticalLayout topLevelLayout = new VerticalLayout();
		topLevelLayout.setMargin(new MarginInfo(false, true, false, true));
		topLevelLayout.setHeightUndefined();
		setCompositionRoot(topLevelLayout);

		Component languageChoice = getLanguageChoiceComponent();
		topLevelLayout.addComponent(languageChoice);
		topLevelLayout.setComponentAlignment(languageChoice, Alignment.TOP_CENTER);
		
		Component authnOptionsComponent = getAuthnOptionsComponent(authenticators);
		topLevelLayout.addComponent(authnOptionsComponent);
		topLevelLayout.setComponentAlignment(authnOptionsComponent, Alignment.MIDDLE_CENTER);
		
		if (outdatedCredentialDialogLauncher.get())
			return;
		
		//Extra safety - it can happen that we entered the UI in pipeline of authentication,
		// if this UI expired in the meantime. Shouldn't happen often as heart of authentication UI
		// is beating very slowly but in case of very slow user we may still need to refresh.
		refreshAuthenticationState(VaadinService.getCurrentRequest());
	}
	
	private Component getLanguageChoiceComponent()
	{
		HorizontalLayout languageChoiceLayout = new HorizontalLayout();
		languageChoiceLayout.setMargin(true);
		languageChoiceLayout.setSpacing(false);
		languageChoiceLayout.setWidth(100, Unit.PERCENTAGE);
		languageChoiceLayout.addComponent(localeChoice);
		languageChoiceLayout.setComponentAlignment(localeChoice, Alignment.MIDDLE_RIGHT);
		return languageChoiceLayout;
	}
	
	private Component getAuthnOptionsComponent(List<AuthenticationOption> authentionOptions)
	{
		VerticalLayout authNSelection = new VerticalLayout();
		authNSelection.setWidthUndefined();
		for (AuthenticationOption authNOption: authentionOptions)
		{
			VaadinAuthentication firstAuthenticator = (VaadinAuthentication) authNOption.getPrimaryAuthenticator();
			Collection<VaadinAuthenticationUI> optionUIInstances = firstAuthenticator.createUIInstance();
			for (VaadinAuthenticationUI vaadinAuthenticationUI : optionUIInstances)
			{
				if (!vaadinAuthenticationUI.isAvailable())
					continue;
				SelectedAuthNPanel authNPanel = new SelectedAuthNPanel(msg, authnProcessor, 
						idsMan, execService, cancelHandler, 
						endpointDescription.getRealm(),
						endpointDescription.getEndpoint().getContextAddress(), 
						unknownUserDialogProvider);
				authNPanel.setAuthenticationListener(new AuthenticationListenerImpl(authNPanel));
				authNPanel.setAuthenticator(vaadinAuthenticationUI, authNOption, vaadinAuthenticationUI.getId());
				authNSelection.addComponent(authNPanel);
				authNSelection.setComponentAlignment(authNPanel, Alignment.TOP_CENTER);
			}
		}
		
		Button registration = buildRegistrationButton();
		if (registration != null)
		{
			authNSelection.addComponent(registration);
			authNSelection.setComponentAlignment(registration, Alignment.TOP_CENTER);
		}
		return authNSelection;
	}

	private Button buildRegistrationButton()
	{
		if (!enableRegistration)
			return null;
		Button register = new Button(msg.getMessage("RegistrationFormChooserDialog.register"));
		register.addStyleName(Styles.vButtonLink.toString());
		register.addClickListener(event -> registrationDialogLauncher.run());
		register.setId("AuthenticationUI.registerButton");
		return register;
	}
	
	private void refreshAuthenticationState(VaadinRequest request) 
	{
		if (authNPanelInProgress != null)
			authNPanelInProgress.refresh(request);
	}
	
	private class AuthenticationListenerImpl implements AuthenticationListener
	{
		private final SelectedAuthNPanel authNPanel;
		
		AuthenticationListenerImpl(SelectedAuthNPanel authNPanel)
		{
			this.authNPanel = authNPanel;
		}

		@Override
		public void authenticationStateChanged(boolean started)
		{
			if (started)
				authNPanelInProgress = authNPanel;
			else
				authNPanelInProgress = null;
		}

		@Override
		public void clearUI()
		{
		}
	}
}
