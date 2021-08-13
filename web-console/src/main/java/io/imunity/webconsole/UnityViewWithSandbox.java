/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole;

import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;

/**
 * 
 * @author P.Piernik
 *
 */
public interface UnityViewWithSandbox extends UnityView
{
	void setSandboxRouter(SandboxAuthnRouter router);
}
