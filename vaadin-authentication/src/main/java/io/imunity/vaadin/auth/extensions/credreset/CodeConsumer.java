/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.extensions.credreset;

import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.exceptions.WrongArgumentException;

@FunctionalInterface
public interface CodeConsumer
{
	void acceptCode(String code) throws TooManyAttempts, WrongArgumentException;
}
