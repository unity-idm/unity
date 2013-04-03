/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

/**
 * Notifies about the display visibility changes. Useful to activate or deactivate default key binding.
 * @author K. Benedyczak
 */
public interface ActivationListener
{
	public void stateChanged(boolean enabled);
}
