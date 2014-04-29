/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import pl.edu.icm.unity.db.export.DumpHeader;
import pl.edu.icm.unity.db.export.IdentitiesIE;
import pl.edu.icm.unity.db.mapper.IdentitiesMapper;
import pl.edu.icm.unity.db.model.IdentityBean;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Updates DB contents. Note that this class is not updating DB schema (it is done in {@link InitDB}).
 * In general the implementation is based on the Import/export features: the elements which should be 
 * updated are exported and then imported what updates their state to the actual version. Finally 
 * the updated object is replacing the previous ones. However tons of exceptions to this schema can be expected.
 *  
 * @author K. Benedyczak
 */
public class ContentsUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, ContentsUpdater.class);
	private final IdentitiesIE identitiesIE;
	
	@Autowired
	public ContentsUpdater(IdentitiesIE identitiesIE)
	{
		this.identitiesIE = identitiesIE;
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

		log.info(" - Identities are recreated");
		JsonFactory jsonF = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(102400);
		JsonGenerator jg = jsonF.createGenerator(baos);
		identitiesIE.serialize(sql, jg);
		
		IdentitiesMapper mapper = sql.getMapper(IdentitiesMapper.class);
		List<IdentityBean> allIds = mapper.getIdentities();
		for (IdentityBean idB: allIds)
			mapper.deleteIdentity(idB.getName());
		
		JsonParser jp = jsonF.createParser(baos.toByteArray());
		DumpHeader fakeHeader = new DumpHeader();
		fakeHeader.setVersionMajor(1);
		fakeHeader.setVersionMinor(0);
		identitiesIE.deserialize(sql, jp, fakeHeader);

		log.info("Update to 2_1_1 format finished");
	}
}
