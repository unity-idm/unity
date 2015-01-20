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
	
	@Autowired
	public ContentsUpdater(IdentitiesIE identitiesIE, GenericsIE genericsIE)
	{
		this.identitiesIE = identitiesIE;
		this.genericsIE = genericsIE;
	}

	public void update(long oldDbVersion, SqlSession sql) throws IOException, EngineException
	{
		if (oldDbVersion < InitDB.dbVersion2Long("2_1_1"))
			updateContents2_1_0To2_1_1(sql);
	}
	
	/**
	 * updates dynamic identities
	 * @throws IOException 
	 * @throws EngineException 
	 */
	private void updateContents2_1_0To2_1_1(SqlSession sql) throws IOException, EngineException
	{
		log.info("Updating DB contents to 2_1_1 format");

		updateIdentitites(sql);
		updateGeneric(sql);
		
		log.info("Update to 2_1_1 format finished");
	}
	
	private void updateIdentitites(SqlSession sql) throws IOException, EngineException
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
		DumpHeader fakeHeader = new DumpHeader();
		fakeHeader.setVersionMajor(1);
		fakeHeader.setVersionMinor(0);
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




