/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.idpselector;

import java.util.Collection;
import java.util.Locale;

/**
 * Description of a remote IDPs.
 * 
 * @author K. Benedyczak
 */
public interface IdPsSpecification
{
	Collection<String> getIdpKeys();
	
	String getIdPName(String key, Locale locale);
	
	String getIdPLogoUri(String key, Locale locale);
}
