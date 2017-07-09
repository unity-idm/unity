/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.pam;

import org.apache.logging.log4j.Logger;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.userimport.UserImportSPI;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;


/**
 * Imports user from a local operating system. Resolves information about the given user
 * using calls to native libc library. 
 * 
 * @author K. Benedyczak
 */
public class OSImporter implements UserImportSPI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP, OSImporter.class);
	
	private String idpName;
	
	public OSImporter(String idpName)
	{
		this.idpName = idpName;
	}

	@Override
	public RemotelyAuthenticatedInput importUser(String identity, String type)
	{
		if (type != UsernameIdentity.ID)
		{
			log.warn("Can not import user of type " + type + 
					" from local OS, only " + UsernameIdentity.ID + 
					" is supported.");
			return null;
		}
		
		try
		{
			UnixUser unixUser = new UnixUser(identity);
			return LibPAMUtils.unixUser2RAI(unixUser, idpName);
		} catch (PAMException e)
		{
			log.warn("Import of user " + identity + " from local OS failed, skipping", e);
			return null;
		}
	}
}
