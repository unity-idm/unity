/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;


public interface CancelHandler
{
	/**
	 * Invoked when cancel is clicked in authentication page.
	 */
	void onCancel();
}