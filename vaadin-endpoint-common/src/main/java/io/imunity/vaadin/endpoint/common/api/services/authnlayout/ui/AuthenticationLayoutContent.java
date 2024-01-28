/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui;

import java.util.List;

import io.imunity.vaadin.elements.LocalizedTextFieldDetails;

/**
 * Contains all ui elements associated with the login screen configuration
 * 
 * @author P.Piernik
 *
 */
public class AuthenticationLayoutContent
{
	public final List<AuthnLayoutColumn> columns;
	public final List<LocalizedTextFieldDetails> separators;

	public AuthenticationLayoutContent(List<AuthnLayoutColumn> columns, List<LocalizedTextFieldDetails> separators)
	{
		this.columns = columns;
		this.separators = separators;
	}

}