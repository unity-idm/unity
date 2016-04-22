/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.userimport;

import pl.edu.icm.unity.server.authn.AuthenticationResult;

/**
 * Internal API for triggering user import.
 * 
 * @author Krzysztof Benedyczak
 */
public interface UserImportSerivce
{
	/**
	 * Perform user import.
	 * @param identity
	 * @param type
	 * @return the returned object can be typically ignored, but is provided if caller is interested in 
	 * the results of import. The object's class is bit misnamed here, but it provides the complete information
	 * which is the same as after remote authentication of a user. 
	 */
	AuthenticationResult importUser(String identity, String type);
}
