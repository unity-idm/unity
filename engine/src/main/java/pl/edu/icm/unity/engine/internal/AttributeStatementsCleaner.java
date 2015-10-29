/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;

/**
 * Invoked periodically to scan group attribute statements, and remove the invalid ones.
 * This is done only to tidy the database - the attribute resolve process is ignoring outdated statements.
 * <p>
 * WARNING if interface is implemented fix transactions
 * @author K. Benedyczak
 */
@Component
public class AttributeStatementsCleaner
{
	private DBGroups dbGroups;
	
	@Autowired
	public AttributeStatementsCleaner(DBGroups dbGroups)
	{
		this.dbGroups = dbGroups;
	}

	@Transactional
	public int updateGroups() throws IllegalGroupValueException, IllegalAttributeTypeException
	{
		return dbGroups.updateAllGroups(SqlSessionTL.get());
	}
}
