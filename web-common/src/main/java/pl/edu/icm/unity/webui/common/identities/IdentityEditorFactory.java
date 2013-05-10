/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities;


/**
 * Implementation are intended to be app singletons, and should provide instances of {@link IdentityEditor}.
 * @author K. Benedyczak
 */
public interface IdentityEditorFactory
{
	public String getSupportedIdentityType();
	
	public IdentityEditor createInstance();

}
