/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Mar 17, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.db;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeSet;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.JsonSerializer;
import pl.edu.icm.unity.db.json.SerializersRegistry;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.mapper.IdentitiesMapper;
import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.IdentityType;
import pl.edu.icm.unity.types.IdentityTypeDefinition;

/**
 * Initializes DB schema and inserts the initial data. It is checked if DB was already initialized.
 * If so no change is commited.
 * @author K. Benedyczak
 */
@Component
public class InitDB
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InitDB.class);

	private DBSessionManager db;
	private IdentityTypesRegistry idTypesReg;
	private JsonSerializer<IdentityType> idTypeSerializer;
	
	@Autowired
	public InitDB(DBSessionManager db, IdentityTypesRegistry idTypesReg, SerializersRegistry serializersReg)
	{
		super();
		this.db = db;
		this.idTypesReg = idTypesReg;
		this.idTypeSerializer = serializersReg.getSerializer(IdentityType.class);
	}

	/**
	 * Drops everything(!!) and recreates the initial DB state.
	 */
	public void resetDatabase()
	{
		log.info("Database will be totally wiped");
		performUpdate("cleardb-");
		log.info("The whole contents removed");
		initDB();
		initData();
	}
	
	public void initIfNeeded() throws FileNotFoundException, IOException, InternalException
	{
		SqlSession session = db.getSqlSession(false);
		try
		{
			session.selectOne("getDBVersion");
			log.info("Database initialized, skipping creation");
		} catch (PersistenceException e)
		{
			initDB();
			initData();
		}
	}
	
	private void performUpdate(String operationPfx)
	{
		Collection<String> ops = new TreeSet<String>(db.getMyBatisConfiguration().getMappedStatementNames());
		SqlSession session = db.getSqlSession(ExecutorType.BATCH, true);
		for (String name: ops)
			if (name.startsWith(operationPfx))
				session.update(name);
		session.commit();
		db.releaseSqlSession(session);		
	}
	
	private void initDB()
	{
		log.info("Initializing DB schema");
		performUpdate("initdb");
		SqlSession session = db.getSqlSession(false);
		try
		{
			session.insert("initVersion");
		} finally
		{
			db.releaseSqlSession(session);		
		}
	}

	private void initData() throws InternalException
	{
		log.info("Inserting initial data");
		SqlSession session = db.getSqlSession(true);
		try
		{
			GroupsMapper groups = session.getMapper(GroupsMapper.class);
			GroupBean root = new GroupBean();
			root.setName(GroupResolver.ROOT_GROUP_NAME);
			groups.insertGroup(root);
			
			createIDTypes(session);
			
			session.commit();
		} finally
		{
			db.releaseSqlSession(session);		
		}
		log.info("Initial data inserted");
	}
	
	private void createIDTypes(SqlSession session)
	{
		IdentitiesMapper mapper = session.getMapper(IdentitiesMapper.class);
		Collection<IdentityTypeDefinition> idTypes = idTypesReg.getAll();
		for (IdentityTypeDefinition idTypeDef: idTypes)
		{
			BaseBean toAdd = new BaseBean();
			IdentityType idType = new IdentityType(idTypeDef);
			idType.setDescription(idTypeDef.getDefaultDescription());
			idType.setExtractedAttributes(idTypeDef.getAttributesSupportedForExtraction());

			toAdd.setName(idTypeDef.getId());
			toAdd.setContents(idTypeSerializer.toJson(idType));
			mapper.insertIdentityType(toAdd);
		}
	}
}
