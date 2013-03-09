/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.InitDB;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.ServerManagement;

/**
 * Implementation of general mainenance.
 * @author K. Benedyczak
 */
@Component
public class ServerManagementImpl implements ServerManagement
{
	private InitDB initDb;
	private EngineInitialization engineInit;

	@Autowired
	public ServerManagementImpl(InitDB initDb, EngineInitialization engineInit)
	{
		this.initDb = initDb;
		this.engineInit = engineInit;
	}


	@Override
	public void resetDatabase() throws EngineException
	{
		initDb.resetDatabase();
		engineInit.start();
	}
}
