/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.List;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.userimport.UserImportSpec;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Allows for manual triggering of user import.
 * @author Krzysztof Benedyczak
 */
public interface UserImportManagement
{
	/**
	 * Perform user import.
	 * @return the returned object can be typically ignored, but is provided if caller is interested in 
	 * the results of import. The object's class is bit misnamed here, but it provides the complete information
	 * which is the same as after remote authentication of a user. Number and order of entries matches the number 
	 * of speced imports. 
	 */
	List<AuthenticationResult> importUser(List<UserImportSpec> imports) throws EngineException;
}
