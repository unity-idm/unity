/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

/**
 * Implemented by panel showing single retrieval UI: both 1st and 2nd factor.
 * @author K. Benedyczak
 */
public interface AuthnPanel 
{
	boolean focusIfPossible();
}
