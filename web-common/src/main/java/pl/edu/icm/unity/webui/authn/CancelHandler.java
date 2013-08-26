/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

/**
 * The implementation is invoked by authentication page, when user clicks cancel.
 * 
 * @author K. Benedyczak
 */
public interface CancelHandler
{
	/**
	 * Invoked when cancel is clicked in authentication page.
	 */
	public void onCancel();
}
