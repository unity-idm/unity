/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import java.io.File;
import java.io.IOException;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import pl.edu.icm.unity.db.DB;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;

/**
 * Responsible for import and export of database state from/to JSON format. 
 * @author K. Benedyczak
 */
@Component
public class ImportExport
{
	@Autowired
	private UnityServerConfiguration configuration;

	@Autowired
	private AttributeTypesIE attributeTypesIE;
	@Autowired
	private IdentityTypesIE identityTypesIE;
	@Autowired
	private EntitiesIE entitiesIE;
	@Autowired
	private IdentitiesIE identitiesIE;
	@Autowired
	private GroupsIE groupsIE;
	@Autowired
	private GroupMembersIE groupMembersIE;
	@Autowired
	private AttributesIE attributesIE;
	@Autowired
	private GenericsIE genericsIE;
	
	/**
	 * Creates a file with a complete database contents.
	 * @return file reference with DB dump.
	 * @throws IOException 
	 * @throws JsonGenerationException 
	 */
	public File exportDB(SqlSession sql) throws JsonGenerationException, IOException, EngineException
	{
		File ret = createExportFile();

		JsonFactory jsonF = new JsonFactory();
		JsonGenerator jg = jsonF.createGenerator(ret, JsonEncoding.UTF8);
		jg.useDefaultPrettyPrinter();
		
		jg.writeStartObject();
		
		String[] dbVer = DB.DB_VERSION.split("_");
		int versionMajor = Integer.parseInt(dbVer[1]);
		int versionMinor = Integer.parseInt(dbVer[2]);
		
		jg.writeNumberField("versionMajor", versionMajor);
		jg.writeNumberField("versionMinor", versionMinor);
		jg.writeNumberField("timestamp", System.currentTimeMillis());

		jg.writeObjectFieldStart("contents");

		jg.writeFieldName("attributeTypes");
		attributeTypesIE.serialize(sql, jg);
		jg.flush();

		jg.writeFieldName("identityTypes");
		identityTypesIE.serialize(sql, jg);
		jg.flush();

		jg.writeFieldName("entities");
		entitiesIE.serialize(sql, jg);
		jg.flush();

		jg.writeFieldName("identities");
		identitiesIE.serialize(sql, jg);
		jg.flush();

		jg.writeFieldName("groups");
		groupsIE.serialize(sql, jg);
		jg.flush();

		jg.writeFieldName("groupMembers");
		groupMembersIE.serialize(sql, jg);
		jg.flush();

		jg.writeFieldName("attributes");
		attributesIE.serialize(sql, jg);
		jg.flush();

		jg.writeFieldName("genericObjects");
		genericsIE.serialize(sql, jg);
		jg.flush();
		
		jg.writeEndObject(); //contents
		jg.writeEndObject(); //root
		jg.close();
		return ret;
	}

	/**
	 * Loads the database contents from the given file.
	 * In principle the database should be cleared before calling this method.
	 * @param from
	 * @throws IOException 
	 * @throws JsonParseException 
	 * @throws EngineException 
	 */
	public void importDB(File from, SqlSession sql) throws JsonParseException, IOException, EngineException
	{
		JsonFactory jsonF = new JsonFactory();
		JsonParser jp = jsonF.createParser(from);
		JsonUtils.nextExpect(jp, JsonToken.START_OBJECT);
		
		DumpHeader header = loadHeader(jp);
		
		JsonUtils.nextExpect(jp, "contents");
		
		JsonUtils.nextExpect(jp, "attributeTypes");
		attributeTypesIE.deserialize(sql, jp);
		
		JsonUtils.nextExpect(jp, "identityTypes");
		identityTypesIE.deserialize(sql, jp);

		JsonUtils.nextExpect(jp, "entities");
		entitiesIE.deserialize(sql, jp);

		JsonUtils.nextExpect(jp, "identities");
		identitiesIE.deserialize(sql, jp, header);

		JsonUtils.nextExpect(jp, "groups");
		groupsIE.deserialize(sql, jp, header);
		
		JsonUtils.nextExpect(jp, "groupMembers");
		groupMembersIE.deserialize(sql, jp);

		JsonUtils.nextExpect(jp, "attributes");
		attributesIE.deserialize(sql, jp);

		JsonUtils.nextExpect(jp, "genericObjects");
		genericsIE.deserialize(sql, jp);
	}

	private DumpHeader loadHeader(JsonParser jp) throws JsonParseException, IOException
	{
		DumpHeader ret = new DumpHeader();
		JsonUtils.nextExpect(jp, "versionMajor");
		ret.setVersionMajor(jp.getIntValue());
		JsonUtils.nextExpect(jp, "versionMinor");
		ret.setVersionMinor(jp.getIntValue());
		JsonUtils.nextExpect(jp, "timestamp");
		ret.setTimestamp(jp.getLongValue());
		return ret;
	}
	
	public File getExportDirectory()
	{
		File workspace = configuration.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true);
		File exportDir = new File(workspace, ServerManagement.DB_DUMP_DIRECTORY);
		if (!exportDir.exists())
			exportDir.mkdir();
		return exportDir;
	}
	
	public String getExportFilePrefix()
	{
		return "export-";
	}
	
	public String getExportFileSuffix()
	{
		return ".json";
	}
	
	private File createExportFile() throws IOException
	{
		File exportDir = getExportDirectory();
		if (exportDir.list().length > 1)
			throw new IOException("Maximum number of database dumps was reached. " +
					"Subsequent dumps can be created in few minutes.");
		return File.createTempFile(getExportFilePrefix(), getExportFileSuffix(), exportDir);
	}
}
