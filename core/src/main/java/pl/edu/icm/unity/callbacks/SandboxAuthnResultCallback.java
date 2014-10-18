package pl.edu.icm.unity.callbacks;

import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;

/**
 * Retrieval must provide an authentication result along with specific data via this callback. 
 * @author Roman Krysinski
 */
public interface SandboxAuthnResultCallback
{
	public void handleAuthnInput(RemotelyAuthenticatedInput input);

	public void handleAuthnError(AuthenticationException e);
	
	public boolean validateProfile();
	
	public void handleProfileValidation(AuthenticationResult authnResult, RemotelyAuthenticatedInput input, StringBuffer capturedLogs);
}
