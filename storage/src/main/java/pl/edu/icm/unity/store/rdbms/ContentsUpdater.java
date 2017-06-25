/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.io.IOException;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Updates DB contents. Note that this class is not updating DB schema (it is done in {@link InitDB}).
 * In general the implementation is based on the Import/export features: the elements which should be 
 * updated are exported and then imported what updates their state to the actual version. Finally 
 * the updated object is replacing the previous ones. However tons of exceptions to this schema can be expected.
 *  
 * @author K. Benedyczak
 */
@Component
public class ContentsUpdater
{
	public void update(long oldDbVersion, SqlSession sql) throws IOException, EngineException
	{
	}
}




