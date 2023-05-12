/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api;

public interface EnquiresDialogLauncher
{
	void showEnquiryDialogIfNeeded(Runnable gotoNextUI);
}
