/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.authn.AccessBlockedDialog;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationStyle;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * The login component used for the 2nd factor authentication 
 * 
 * @author K. Benedyczak
 */
class SecondFactorAuthNPanel extends CustomComponent implements AuthnPanel
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SecondFactorAuthNPanel.class);
	private UnityMessageSource msg;
	private WebAuthenticationProcessor authnProcessor;
	private EntityManagement idsMan;
	private AuthenticationUIController currentAuthnResultCallback;
	private ExecutorsService execService;
	private String clientIp;
	private AuthenticationRealm realm;
	private AuthenticationListener authNListener;
	private final Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider; 
	private final Supplier<Boolean> rememberMeProvider;
	
	private VerticalLayout authenticatorContainer;
	private AuthenticationOption selectedAuthnOption;
	
	
	SecondFactorAuthNPanel(UnityMessageSource msg, WebAuthenticationProcessor authnProcessor,
			EntityManagement idsMan, ExecutorsService execService,
			CancelHandler cancelHandler, AuthenticationRealm realm,
			Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider,
			Supplier<Boolean> rememberMeProvider)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.idsMan = idsMan;
		this.execService = execService;
		this.realm = realm;
		this.unknownUserDialogProvider = unknownUserDialogProvider;
		this.rememberMeProvider = rememberMeProvider;

		authenticatorContainer = new VerticalLayout();		
		authenticatorContainer.setHeight(100, Unit.PERCENTAGE);
		authenticatorContainer.setWidth(100, Unit.PERCENTAGE);
		authenticatorContainer.setSpacing(true);
		authenticatorContainer.setMargin(false);
		authenticatorContainer.addStyleName("u-authn-component");
		setCompositionRoot(authenticatorContainer);
	}

	public void setAuthenticator(VaadinAuthenticationUI secondaryUI, PartialAuthnState partialState)
	{
		this.selectedAuthnOption = partialState.getAuthenticationOption();
		secondaryUI.clear();
		AuthenticationUIController authnResultCallback = createSecondaryAuthnResultCallback(secondaryUI, 
				partialState);
		addRetrieval(secondaryUI, authnResultCallback);
		try
		{
			secondaryUI.presetEntity(resolveEntity(partialState.getPrimaryResult()));
		} catch (EngineException e)
		{
			log.error("Can't resolve the first authenticated entity", e);
		}
		
		
		Button resetMfaButton = new Button(msg.getMessage("AuthenticationUI.resetMfaButton"));
		resetMfaButton.setDescription(msg.getMessage("AuthenticationUI.resetMfaButtonDesc"));
		resetMfaButton.addStyleName(Styles.vButtonLink.toString());
		resetMfaButton.addStyleName("u-authn-resetMFAButton");
		resetMfaButton.addClickListener(event -> switchToPrimaryAuthentication());
		authenticatorContainer.addComponent(resetMfaButton);
		authenticatorContainer.setComponentAlignment(resetMfaButton, Alignment.TOP_RIGHT);
	}
	
	protected void handleError(String genericError, String authenticatorError)
	{
		if (authenticatorContainer.getComponentCount() > 0)
			updateFocus(authenticatorContainer.getComponent(0));
		String errorToShow = authenticatorError == null ? genericError : authenticatorError;
		NotificationPopup.showError(msg, msg.getMessage("AuthenticationUI.authnErrorTitle"), errorToShow);
		showWaitScreenIfNeeded();
	}
	
	/**
	 * Resets the authentication UI to the initial state
	 */
	private void switchToPrimaryAuthentication()
	{
		if (authNListener != null)
			authNListener.switchBackToFirstFactor();
	}

	private Entity resolveEntity(AuthenticationResult unresolved) throws EngineException
	{
		AuthenticatedEntity ae = unresolved.getAuthenticatedEntity();
		return idsMan.getEntity(new EntityParam(ae.getEntityId()));
	}
	
	private boolean updateFocus(Component retrievalComponent)
	{
		if (retrievalComponent instanceof Focusable)
		{
			((Focusable)retrievalComponent).focus();
			return true;
		}
		return false;
	}
	
	private void addRetrieval(VaadinAuthenticationUI authnUI, AuthenticationUIController handler)
	{
		Component retrievalComponent = authnUI.getComponent();
		authenticatorContainer.addComponent(retrievalComponent);
		authnUI.setAuthenticationCallback(handler);
		currentAuthnResultCallback = handler;
	}
	
	private AuthenticationUIController createSecondaryAuthnResultCallback(VaadinAuthenticationUI secondaryUI,
			PartialAuthnState partialState)
	{
		return new SecondaryAuthenticationResultCallbackImpl(secondaryUI, partialState);
	}
	
	private void onAuthenticationStart(AuthenticationStyle style)
	{
		clientIp = VaadinService.getCurrentRequest().getRemoteAddr();
	}

	private void showWaitScreenIfNeeded()
	{
		UnsuccessfulAuthenticationCounter counter = WebAuthenticationProcessor.getLoginCounter();
		if (counter.getRemainingBlockedTime(clientIp) > 0)
		{
			AccessBlockedDialog dialog = new AccessBlockedDialog(msg, execService);
			dialog.show();
			return;
		}
	}
	
	/**
	 * Collects authN results from the 2nd authenticator. Afterwards, the final authentication result 
	 * processing is launched.
	 */
	private class SecondaryAuthenticationResultCallbackImpl implements AuthenticationUIController
	{
		private final PartialAuthnState partialState;
		private final VaadinAuthenticationUI authnUI;
		
		public SecondaryAuthenticationResultCallbackImpl(VaadinAuthenticationUI authnUI,
				PartialAuthnState partialState)
		{
			this.authnUI = authnUI;
			this.partialState = partialState;
		}

		@Override
		public void onCompletedAuthentication(AuthenticationResult result)
		{
			processAuthn(result, null);
		}
		

		@Override
		public void onFailedAuthentication(AuthenticationResult result, String error,
				Optional<String> errorDetail)
		{
			processAuthn(result, error);
		}
		
		private void processAuthn(AuthenticationResult result, String error)
		{
			log.trace("Received authentication result of the 2nd authenticator" + result);
			try
			{
				authnProcessor.processSecondaryAuthnResult(partialState, result, clientIp, realm, 
						selectedAuthnOption, rememberMeProvider.get());
			} catch (AuthenticationException e)
			{
				log.trace("Secondary authentication failed ", e);
				handleError(msg.getMessage(e.getMessage()), null);
				switchToPrimaryAuthentication();
			}
		}
		
		@Override
		public void onStartedAuthentication(AuthenticationStyle style)
		{
			onAuthenticationStart(style);
		}

		@Override
		public void onCancelledAuthentication()
		{
		}
		
		@Override
		public void refresh(VaadinRequest request)
		{
			authnUI.refresh(request);
		}
	}

	protected void handleUnknownUser(UnknownRemoteUserException e)
	{
		if (e.getFormForUser() != null || e.getResult().isEnableAssociation())
		{
			log.trace("Authentication successful, user unknown, "
					+ "showing unknown user dialog");
			showUnknownUserDialog(e);
		} else
		{
			log.trace("Authentication successful, user unknown, "
					+ "no registration form");
			handleError(msg.getMessage("AuthenticationUI.unknownRemoteUser"), null);
		}
	}

	protected void showUnknownUserDialog(UnknownRemoteUserException ee)
	{
		UnknownUserDialog dialog = unknownUserDialogProvider.apply(ee.getResult()); 
		dialog.show();
	}	
	
	public void refresh(VaadinRequest request)
	{
		if (currentAuthnResultCallback != null)
			currentAuthnResultCallback.refresh(request);
	}
	
	@Override
	public boolean focusIfPossible()
	{
		if (authenticatorContainer.getComponentCount() == 0)
			return false;
		return updateFocus(authenticatorContainer.getComponent(0));
	}

	/**
	 * listener to be registered to the authentication button.
	 * @param listener
	 */
	public void setAuthenticationListener(AuthenticationListener listener)
	{
		this.authNListener = listener;
	}
	
	/**
	 * Used by upstream code holding this component to be informed about changes in this component. 
	 */
	public interface AuthenticationListener
	{
		void switchBackToFirstFactor();
	}
	
	/**
	 * Internal operations which are used to pass actions to the selected {@link VaadinAuthenticationUI}
	 */
	public interface AuthenticationUIController extends AuthenticationCallback
	{
		void refresh(VaadinRequest request);
	}
}
