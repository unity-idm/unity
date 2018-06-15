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
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.authn.AccessBlockedDialog;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.PreferredAuthenticationHelper;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationStyle;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * The login component of the 1st factor authentication. Wraps a single Vaadin retrieval UI and connects 
 * it to the authentication screen.  
 * 
 * @author K. Benedyczak
 */
class PrimaryAuthNPanel extends CustomComponent implements AuthnPanel
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, PrimaryAuthNPanel.class);
	private UnityMessageSource msg;
	private WebAuthenticationProcessor authnProcessor;
	private AuthenticationUIController currentAuthnResultCallback;
	private ExecutorsService execService;
	private String clientIp;
	private AuthenticationRealm realm;
	private AuthenticationListener authNListener;
	private final Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider; 
	
	private VerticalLayout authenticatorContainer;
	private AuthenticationOption selectedAuthnOption;
	private String authnId;
	private String endpointPath;
	private Supplier<Boolean> rememberMeProvider;
	private boolean gridCompatible;
	
	
	PrimaryAuthNPanel(UnityMessageSource msg, WebAuthenticationProcessor authnProcessor,
			ExecutorsService execService,
			CancelHandler cancelHandler, AuthenticationRealm realm,
			String endpointPath, 
			Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider,
			Supplier<Boolean> rememberMeProvider,
			boolean gridCompatible)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.execService = execService;
		this.realm = realm;
		this.endpointPath = endpointPath;
		this.unknownUserDialogProvider = unknownUserDialogProvider;
		this.rememberMeProvider = rememberMeProvider;
		this.gridCompatible = gridCompatible;

		authenticatorContainer = new VerticalLayout();		
		authenticatorContainer.setHeight(100, Unit.PERCENTAGE);
		authenticatorContainer.setWidth(100, Unit.PERCENTAGE);
		authenticatorContainer.setSpacing(false);
		authenticatorContainer.setMargin(false);
		authenticatorContainer.addStyleName("u-authn-component");
		setCompositionRoot(authenticatorContainer);
	}

	public void setAuthenticator(VaadinAuthenticationUI primaryAuthnUI, AuthenticationOption authnOption, 
			String id)
	{
		this.selectedAuthnOption = authnOption;
		this.authnId = id;
		primaryAuthnUI.clear();
		AuthenticationUIController primaryAuthnResultCallback = createPrimaryAuthnResultCallback(primaryAuthnUI);
		authenticatorContainer.removeAllComponents();
		addRetrieval(primaryAuthnUI, primaryAuthnResultCallback);
	}
	
	protected void handleError(String genericError, String authenticatorError)
	{
		setNotAuthenticating();
		if (authenticatorContainer.getComponentCount() > 0)
			updateFocus(authenticatorContainer.getComponent(0));
		String errorToShow = authenticatorError == null ? genericError : authenticatorError;
		NotificationPopup.showError(msg, msg.getMessage("AuthenticationUI.authnErrorTitle"), errorToShow);
		showWaitScreenIfNeeded();
	}
	
	public String getAuthenticationOptionId()
	{
		return authnId;
	}
	
	/**
	 * Clears the UI so a new authentication can be started.
	 */
	private void setNotAuthenticating()
	{
		if (authNListener != null)
			authNListener.authenticationStopped();
	}
	
	private void switchToSecondaryAuthentication(PartialAuthnState partialState)
	{
		if (authNListener != null)
			authNListener.switchTo2ndFactor(partialState);
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
		Component retrievalComponent = gridCompatible ? authnUI.getGridCompatibleComponent() : authnUI.getComponent();
		authenticatorContainer.addComponent(retrievalComponent);
		authnUI.setAuthenticationCallback(handler);
		currentAuthnResultCallback = handler;
	}
	
	/**
	 * The method is separated as can be overridden in sandbox authn. 
	 * @return primary authentication result callback.
	 */
	private AuthenticationUIController createPrimaryAuthnResultCallback(VaadinAuthenticationUI primaryAuthnUI)
	{
		return new PrimaryAuthenticationResultCallbackImpl(primaryAuthnUI);
	}

	private void onAuthenticationStart(AuthenticationStyle style)
	{
		clientIp = VaadinService.getCurrentRequest().getRemoteAddr();
		if (authNListener != null && style != AuthenticationStyle.IMMEDIATE)
			authNListener.authenticationStarted(style == AuthenticationStyle.WITH_EXTERNAL_CANCEL);
		setLastIdpCookie(authnId);
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
	
	private void setLastIdpCookie(String idpKey)
	{
		if (endpointPath == null)
			return;
		VaadinResponse resp = VaadinService.getCurrentResponse();
		resp.addCookie(PreferredAuthenticationHelper.createLastIdpCookie(endpointPath, idpKey));
	}
	
	/**
	 * Collects authN result from the first authenticator of the selected {@link AuthenticationOption} 
	 * and process it: manages state of the rest of the UI (cancel button, notifications, registration) 
	 * and if needed proceeds to 2nd authenticator. 
	 * 
	 * @author K. Benedyczak
	 */
	protected class PrimaryAuthenticationResultCallbackImpl implements AuthenticationUIController
	{
		protected VaadinAuthenticationUI authnUI;
		protected boolean authnDone = false;
		
		public PrimaryAuthenticationResultCallbackImpl(VaadinAuthenticationUI authnUI)
		{
			this.authnUI = authnUI;
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
			log.trace("Received authentication result of the primary authenticator " + result);
			authnDone = true;
			try
			{
				PartialAuthnState partialState = authnProcessor.processPrimaryAuthnResult(
						result, clientIp, realm, 
						selectedAuthnOption, rememberMeProvider.get());
				if (partialState == null)
				{
					setNotAuthenticating();
				} else
				{
					switchToSecondaryAuthentication(partialState);
				}
			} catch (UnknownRemoteUserException e)
			{
				handleUnknownUser(e);
			} catch (AuthenticationException e)
			{
				log.trace("Authentication failed ", e);
				handleError(msg.getMessage(e.getMessage()), error);
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
			setNotAuthenticating();
		}
		
		@Override
		public void refresh(VaadinRequest request)
		{
			authnUI.refresh(request);
		}
		
		@Override
		public void cancel()
		{
			authnUI.clear();
		}

		@Override
		public void clear()
		{
			authnUI.clear();
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
		setNotAuthenticating();
		UnknownUserDialog dialog = unknownUserDialogProvider.apply(ee.getResult()); 
		dialog.show();
	}	
	
	public void refresh(VaadinRequest request)
	{
		if (currentAuthnResultCallback != null)
			currentAuthnResultCallback.refresh(request);
	}
	
	public void cancel()
	{
		if (currentAuthnResultCallback != null)
			currentAuthnResultCallback.cancel();
	}
	
	public void clear()
	{
		if (currentAuthnResultCallback != null)
			currentAuthnResultCallback.clear();
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
		void authenticationStarted(boolean showProgress);
		void authenticationStopped();
		void switchTo2ndFactor(PartialAuthnState partialState);
	}
	
	/**
	 * Internal operations which are used to pass actions to the selected {@link VaadinAuthenticationUI}
	 */
	public interface AuthenticationUIController extends AuthenticationCallback
	{
		void cancel();
		void clear();
		void refresh(VaadinRequest request);
	}
}
