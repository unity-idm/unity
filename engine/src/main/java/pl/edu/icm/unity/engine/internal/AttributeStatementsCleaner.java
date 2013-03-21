/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBSessionManager;

/**
 * Invoked periodically to scan group attribute statements, and remove the invalid ones.
 * This is done only to tidy the database - the attribute resolve process is ignoring outdated statements.
 * 
 * @author K. Benedyczak
 */
@Component
public class AttributeStatementsCleaner
{
	private DBSessionManager db;
	private DBGroups dbGroups;
	
	@Autowired
	public AttributeStatementsCleaner(DBSessionManager db, DBGroups dbGroups)
	{
		this.db = db;
		this.dbGroups = dbGroups;
	}


	public int updateGroups()
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			int ret = dbGroups.updateAllGroups(sql);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
}
