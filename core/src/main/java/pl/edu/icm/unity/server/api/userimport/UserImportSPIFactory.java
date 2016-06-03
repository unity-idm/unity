/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.userimport;

import java.util.Properties;

/**
 * Factory of {@link UserImportSPI}.
 *  
 * @author Krzysztof Benedyczak
 */
public interface UserImportSPIFactory
{
	/**
	 * @return implementation name
	 */
	String getName();

	/**
	 * @param configuration
	 * @param idpName
	 * @return a new configured instance
	 */
	UserImportSPI getInstance(Properties configuration, String idpName);
}
