/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationProcessor.PartialAuthnState;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.SelectedAuthNPanel;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormLauncher;

/**
 * Extension of {@link AuthenticatorSetComponent}. Merely disables final authentication in the system after user's 
 * authentication (i.e. the process is stopped just before the very last step).
 * @author K. Benedyczak
 */
public class SandboxSelectedAuthNPanel extends SelectedAuthNPanel
{
	public SandboxSelectedAuthNPanel(UnityMessageSource msg, WebAuthenticationProcessor authnProcessor,
			IdentitiesManagement idsMan,
			InsecureRegistrationFormLauncher formLauncher, ExecutorsService execService,
			final CancelHandler cancelHandler, AuthenticationRealm realm)
	{
		super(msg, authnProcessor, idsMan, formLauncher, execService, cancelHandler, realm,
				null, null, null);
	}

	@Override
	protected AuthenticationHandler createPrimaryAuthnResultCallback(VaadinAuthenticationUI primaryAuthnUI)
	{
		return new AuthenticationResultSandboxCallbackImpl(primaryAuthnUI);
	}

	@Override
	protected AuthenticationHandler createSecondaryAuthnResultCallback(VaadinAuthenticationUI secondaryUI,
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
		public void setAuthenticationResult(AuthenticationResult result)
		{
			authnDone = true;
			setNotAuthenticating();
		}
	}
}
