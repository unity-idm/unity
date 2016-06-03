/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.userimport;

import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;

/**
 * User import implementation must implement this interface.
 *  
 * @author Krzysztof Benedyczak
 */
public interface UserImportSPI
{
	/**
	 * Should perform the import of the user in implementation defined way. Note that the 
	 * engine code provides caching so the implementation should not additionally cache results. 
	 * 
	 * @param identity identity of the user to be imported. 
	 * @param type unity type of the user or null if not known
	 * @return null if the user was not found or raw information about the user obtained during import. 
	 */
	RemotelyAuthenticatedInput importUser(String identity, String type);
}
