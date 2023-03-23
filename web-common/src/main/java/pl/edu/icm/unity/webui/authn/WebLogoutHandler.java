/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

public interface WebLogoutHandler
{
	void logout();

	void logout(boolean soft);
	void logout(boolean soft, String logoutRedirectPath);
}