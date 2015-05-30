package pl.edu.icm.unity.server.authn.remote;

import pl.edu.icm.unity.server.authn.SandboxAuthnContext;


/**
 * Callback used to provide a result of sandboxed authentication.  
 * @author Roman Krysinski
 */
public interface SandboxAuthnResultCallback
{
	public void sandboxedAuthenticationDone(SandboxAuthnContext ctx);
}
