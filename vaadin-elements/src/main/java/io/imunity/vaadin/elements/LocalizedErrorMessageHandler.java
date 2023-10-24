/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import java.util.Locale;

public interface LocalizedErrorMessageHandler
{
	void setErrorMessage(Locale locale, String errorMessage);
}
