/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api;

import java.util.List;

import pl.edu.icm.unity.base.registration.layout.FormParameterElement;

public interface RemoteRegistrationSignupResolver
{
	List<RemoteRegistrationOption> getOptions(FormParameterElement element, boolean enabled);
	RemoteRegistrationGrid getGrid(boolean enabled, int height);
}
