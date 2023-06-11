/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.extensions.credreset;

import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.engine.api.authn.TooManyAttempts;

@FunctionalInterface
public interface CodeConsumer
{
	void acceptCode(String code) throws TooManyAttempts, WrongArgumentException;
}
