package pl.edu.icm.unity.engine.api.authn.remote;

import pl.edu.icm.unity.engine.api.authn.SandboxAuthnContext;


/**
 * Callback used to provide a result of sandboxed authentication.  
 * @author Roman Krysinski
 */
public interface SandboxAuthnResultCallback
{
	void sandboxedAuthenticationDone(SandboxAuthnContext ctx);
}
