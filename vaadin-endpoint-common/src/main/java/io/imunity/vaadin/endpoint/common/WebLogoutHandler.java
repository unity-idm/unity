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

	/**
	 * Synchronously destroys the current session and redirects, without waiting for a browser
	 * round trip. Intended for flows where the caller must guarantee the session is gone before
	 * proceeding (e.g. removing the underlying entity right after). Synchronous peer logout
	 * (LOGOUT_MODE=internalAndSyncPeers) is still performed; the browser-redirect-based peer
	 * logout (internalAndAsyncPeers) can't be done synchronously and is skipped.
	 */
	void logoutImmediately(String logoutRedirectPath);
}