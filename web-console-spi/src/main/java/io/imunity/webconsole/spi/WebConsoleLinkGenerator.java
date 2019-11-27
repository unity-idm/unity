/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.spi;

import com.vaadin.ui.Button;

/**
 * Generates link button to web console views
 * 
 * @author P.Piernik
 *
 */
public interface WebConsoleLinkGenerator
{
	Button editRegistrationForm(String formName);
}
