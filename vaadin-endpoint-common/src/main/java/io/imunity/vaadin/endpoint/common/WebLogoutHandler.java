/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;

public interface WebLogoutHandler
{
	void logout();

	void logout(boolean soft);
	void logout(boolean soft, String logoutRedirectPath);
}