/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import java.util.Map;

import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.webui.authn.AuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.AuthenticatorSetComponent;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.UsernameComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormLauncher;

/**
 * Extension of {@link AuthenticatorSetComponent}. Merely disables final authentication in the system after user's 
 * authentication (i.e. the process is stopped just before the very last step).
 * @author K. Benedyczak
 */
public class SandboxAuthenticatorSetComponent extends AuthenticatorSetComponent
{
	public SandboxAuthenticatorSetComponent(Map<String, VaadinAuthenticationUI> authenticators,
			AuthenticatorSet set, UnityMessageSource msg,
			AuthenticationProcessor authnProcessor,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService, CancelHandler cancelHandler,
			AuthenticationRealm realm)
	{
		super(authenticators, set, msg, authnProcessor, formLauncher, execService, cancelHandler, realm);
	}

	@Override
	protected AuthenticationResultProcessor createAuthnResultCallback(
			Map<String, VaadinAuthenticationUI> authenticators, UsernameComponent usernameComponent)
	{
		return new AuthenticationResultSandboxCallbackImpl(authenticators, usernameComponent);
	}
	
	protected class AuthenticationResultSandboxCallbackImpl extends AuthenticationResultCallbackImpl
	{
		public AuthenticationResultSandboxCallbackImpl(
				Map<String, VaadinAuthenticationUI> authenticators,
				UsernameComponent usernameComp)
		{
			super(authenticators, usernameComp);
		}

		protected void authnDone()
		{
			cleanAuthentication();
			showAuthnProgress(false);
		}
	}
}
