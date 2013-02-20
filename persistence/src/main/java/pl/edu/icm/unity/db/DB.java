/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Mar 17, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.db;

import java.io.IOException;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.mapper.InitdbMapper;
import pl.edu.icm.unity.db.model.DBLimits;
import pl.edu.icm.unity.exceptions.InternalException;


/**
 * Provides general DB initialization, which is run at each startup. 
 * If needed activates creation of DB schema using {@link InitDB}.
 * Provides DB limits and checks if the schema version is correct.
 * 
 * @author K. Benedyczak
 */
@Component
public class DB
{
	public static final String DB_VERSION = "2_0_0";
	
	private DBLimits limits;

	@Autowired
	public DB(DBSessionManager sessionMan, InitDB initDB) throws InternalException, IOException
	{
		initDB.initIfNeeded();
		verifyDBVersion(sessionMan);
		limits = establishDBLimits(sessionMan);
	}
	
	private void verifyDBVersion(DBSessionManager sessionMan) throws InternalException
	{
		String actualDbVersion;
		try
		{
			actualDbVersion = checkCurrentVersion(sessionMan);
		} catch (Exception e)
		{
			throw new InternalException("Can't read version of the database. " +
				"Have you initialized it? Are connection details correctly " +
				"entered in configuration? The error was:\n\n" + e, e);
		}
		if (!actualDbVersion.equals(DB.DB_VERSION))
			throw new InternalException("The database is initialized with " +
				"wrong schema. It is of UVOS version: " + actualDbVersion + 
				" while you are using now UVOS version:" + DB.DB_VERSION);
	}
	
	public DBLimits getDBLimits()
	{
		return limits;
	}
	
	public String checkCurrentVersion(DBSessionManager sessionMan) throws Exception
	{
		SqlSession sqlMap = sessionMan.getSqlSession(false);
		try
		{
			return sqlMap.selectOne("getDBVersion");
		} finally
		{
			sessionMan.releaseSqlSession(sqlMap);
		}
	}
	
	private DBLimits establishDBLimits(DBSessionManager sessionMan) throws InternalException
	{
		SqlSession sqlMap = sessionMan.getSqlSession(false);
		try
		{
			InitdbMapper mapper = sqlMap.getMapper(InitdbMapper.class);
			return mapper.getDBLimits();
		} catch (PersistenceException e)
		{
			throw new InternalException("Can't establish DB limits", e);
		} finally
		{
			sessionMan.releaseSqlSession(sqlMap);
		}
	}

}
