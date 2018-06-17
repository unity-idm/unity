/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import java.util.function.Function;
import java.util.function.Supplier;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;
import pl.edu.icm.unity.webui.authn.tile.SelectedAuthNPanel;

/**
 * Extension of {@link AuthenticatorSetComponent}. Merely disables final authentication in the system after user's 
 * authentication (i.e. the process is stopped just before the very last step).
 * @author K. Benedyczak
 */
public class SandboxSelectedAuthNPanel extends SelectedAuthNPanel
{
	public SandboxSelectedAuthNPanel(UnityMessageSource msg, WebAuthenticationProcessor authnProcessor,
			EntityManagement idsMan, ExecutorsService execService,
			CancelHandler cancelHandler, AuthenticationRealm realm,
			String endpointPath, 
			Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider,
			Supplier<Boolean> rememberMeProvider)
	{
		super(msg, authnProcessor, idsMan, execService, cancelHandler, realm,
				null, null, null);
	}

	@Override
	protected AuthenticationUIController createPrimaryAuthnResultCallback(VaadinAuthenticationUI primaryAuthnUI)
	{
		return new AuthenticationResultSandboxCallbackImpl(primaryAuthnUI);
	}

	@Override
	protected AuthenticationUIController createSecondaryAuthnResultCallback(VaadinAuthenticationUI secondaryUI,
			PartialAuthnState partialState)
	{
		throw new IllegalStateException("This method should never be called in sandboxed authN");
	}
	
	protected class AuthenticationResultSandboxCallbackImpl extends PrimaryAuthenticationResultCallbackImpl
	{
		public AuthenticationResultSandboxCallbackImpl(VaadinAuthenticationUI authnUI)
		{
			super(authnUI);
		}

		@Override
		protected void processAuthn(AuthenticationResult result, String error)
		{
			authnDone = true;
			setNotAuthenticating();
		}
	}
}
