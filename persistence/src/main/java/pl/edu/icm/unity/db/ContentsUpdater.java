/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.export.DumpHeader;
import pl.edu.icm.unity.db.export.GenericsIE;
import pl.edu.icm.unity.db.export.GroupsIE;
import pl.edu.icm.unity.db.export.IdentitiesIE;
import pl.edu.icm.unity.db.mapper.GenericMapper;
import pl.edu.icm.unity.db.mapper.IdentitiesMapper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.utils.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

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
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, ContentsUpdater.class);
	private final IdentitiesIE identitiesIE;
	private GenericsIE genericsIE;
	private GroupsIE groupsIE;
	
	@Autowired
	public ContentsUpdater(IdentitiesIE identitiesIE, GenericsIE genericsIE, GroupsIE groupsIE)
	{
		this.identitiesIE = identitiesIE;
		this.genericsIE = genericsIE;
		this.groupsIE = groupsIE;
	}

	public void update(long oldDbVersion, SqlSession sql) throws IOException, EngineException
	{
		if (oldDbVersion < InitDB.dbVersion2Long("2_1_1"))
			updateGeneric(sql);
		if (oldDbVersion < InitDB.dbVersion2Long("2_1_3"))
			updateIdentitites(sql, headerForVersion(oldDbVersion));
		if (oldDbVersion < InitDB.dbVersion2Long("2_1_5"))
		{
			log.info(" - Updating group attribute statements");
			groupsIE.updateGroupStatements(sql);
		}
	}
	
	private DumpHeader headerForVersion(long version)
	{
		DumpHeader fakeHeader = new DumpHeader();
		
		fakeHeader.setVersionMajor((int)((version/100)%100));
		fakeHeader.setVersionMinor((int)(version%100));
		return fakeHeader;
	}
	
	private void updateIdentitites(SqlSession sql, DumpHeader fakeHeader) throws IOException, EngineException
	{
		log.info(" - Identities are recreated");
		JsonFactory jsonF = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(102400);
		JsonGenerator jg = jsonF.createGenerator(baos);
		identitiesIE.serialize(sql, jg);
		jg.close();
		String contents = baos.toString("UTF-8");
		IdentitiesMapper mapper = sql.getMapper(IdentitiesMapper.class);
		mapper.deleteAllIdentities();
		
		JsonParser jp = jsonF.createParser(contents);
		jp.nextToken();
		identitiesIE.deserialize(sql, jp, fakeHeader);
	}
	
	private void updateGeneric(SqlSession sql) throws IOException, EngineException
	{
		log.info(" - Generics are recreated");
		JsonFactory jsonF = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(102400);
		JsonGenerator jg = jsonF.createGenerator(baos);
		genericsIE.serialize(sql, jg);
		jg.close();
		String contents = baos.toString("UTF-8");
		GenericMapper mapper = sql.getMapper(GenericMapper.class);
		mapper.deleteAll();
		
		JsonParser jp = jsonF.createParser(contents);
		jp.nextToken();
		genericsIE.deserialize(sql, jp);
	}
}




