/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.userimport;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.UserImportManagement;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce.ImportResult;
import pl.edu.icm.unity.engine.api.userimport.UserImportSpec;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Implements triggering of user import - performs authz and delegates to the internal service.
 * @author K. Benedyczak
 */
@Component
@Primary
public class UserImportManagementImpl implements UserImportManagement
{
	private InternalAuthorizationManager authz;
	private UserImportSerivce importService;
	
	@Autowired
	public UserImportManagementImpl(InternalAuthorizationManager authz, UserImportSerivce importService)
	{
		this.authz = authz;
		this.importService = importService;
	}


	@Override
	public List<ImportResult> importUser(List<UserImportSpec> imports) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return importService.importUser(imports);
	}
}
