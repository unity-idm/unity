/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api;

import pl.edu.icm.unity.base.registration.layout.FormParameterElement;

import java.util.List;

public interface RemoteRegistrationSignupHandler
{
	List<RemoteRegistrationOption> getOptions(FormParameterElement element, boolean enabled);
	RemoteRegistrationGrid getGrid(boolean enabled, int height);
	boolean performAutomaticRemoteSignupIfNeeded();
}
