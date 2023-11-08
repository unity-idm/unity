/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes;

public enum ConfirmationEditMode
{
	ADMIN(false, true), 
	USER(true, true), 
	OFF(false, false),
	/**
	 * Editor must guarantee confirmation of returned value, otherwise the value must be assumed invalid.
	 * This option is relevant only for editors which can confirm the value using synchronous pattern.
	 * If synchronous confirmation is not implemented, then this option should be treated as OFF.
	 */
	FORCE_CONFIRMED_IF_SYNC(false, false);
	
	private final boolean showVerifyButton;
	private final boolean showConfirmationStatus;
	
	private ConfirmationEditMode(boolean shouldShowVerifyButton, boolean shouldShowConfirmStatus)
	{
		this.showVerifyButton = shouldShowVerifyButton;
		this.showConfirmationStatus = shouldShowConfirmStatus;
	}

	public boolean isShowVerifyButton()
	{
		return showVerifyButton;
	}

	public boolean isShowConfirmationStatus()
	{
		return showConfirmationStatus;
	}
}