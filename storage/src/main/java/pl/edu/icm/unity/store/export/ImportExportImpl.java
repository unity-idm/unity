/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.AppDataSchemaVersion;
import pl.edu.icm.unity.store.api.ImportExport;

/**
 * Import/export functionality. 
 * @author K. Benedyczak
 */
@Component
public class ImportExportImpl implements ImportExport
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, ImportExportImpl.class);
	private ObjectMapper objectMapper;
	private DumpUpdater updater;
	private List<AbstractIEBase<?>> implementations;
	private JsonFactory jsonF;
	
	@Autowired
	public ImportExportImpl(ObjectMapper objectMapper, DumpUpdater updater,
			List<AbstractIEBase<?>> implementations)
	{
		this.objectMapper = objectMapper;
		this.updater = updater;
		this.implementations = implementations;
		Set<String> names = new HashSet<>();
		Set<Integer> orderKeys = new HashSet<>();
		for (AbstractIEBase<?> i: implementations)
		{
			if (!names.add(i.getStoreKey()))
				throw new IllegalStateException("BUG: IE implementation for object " + 
						i.getStoreKey() + " is duplicated");
			if (!orderKeys.add(i.getSortKey()))
				throw new IllegalStateException("BUG: 2 IE implementations use the same order " + 
						i.getSortKey());
		}
		
		Collections.sort(implementations, (i1, i2) ->
			i1.getSortKey() < i2.getSortKey() ? -1 : 1);
		
		jsonF = new JsonFactory(objectMapper);
		jsonF.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
	}

	@Override
	public void store(OutputStream os) throws IOException
	{
		storeWithVersion(os, AppDataSchemaVersion.CURRENT.getJsonDumpVersion());
	}
	
	@Override
	public void storeWithVersion(OutputStream os, int version) throws IOException
	{
		JsonFactory jsonF = new JsonFactory(objectMapper);
		JsonGenerator jg = jsonF.createGenerator(os, JsonEncoding.UTF8);
		jg.useDefaultPrettyPrinter();
		
		jg.writeStartObject();
		
		jg.writeNumberField("versionMajor", version);
		jg.writeNumberField("versionMinor", 0);
		jg.writeNumberField("timestamp", System.currentTimeMillis());

		jg.writeObjectFieldStart("contents");

		for (AbstractIEBase<?> impl: implementations)
		{
			jg.writeFieldName(impl.getStoreKey());
			impl.serialize(jg);
			jg.flush();
		}

		jg.writeEndObject(); //contents
		jg.writeEndObject(); //root
		jg.close();
	}

	@Override
	public void load(InputStream is) throws IOException
	{
		if (!is.markSupported())
			throw new IllegalArgumentException("Only input streams with mark/reset support can "
					+ "be used to load imported data");
		is.mark(1000);
		JsonParser jp = jsonF.createParser(is);
		DumpHeader header = loadHeader(jp);
		jp.close();
		is.reset();
		
		InputStream isUpdated = updater.update(is, header);

		JsonParser jp2 = jsonF.createParser(isUpdated);
		loadHeader(jp2);
		
		JsonUtils.nextExpect(jp2, "contents");
		
		for (AbstractIEBase<?> impl: implementations)
		{
			log.info("Importing " + impl.getStoreKey());
			JsonUtils.nextExpect(jp2, impl.getStoreKey());
			impl.deserialize(jp2);
		}
		jp2.close();
	}
	
	private DumpHeader loadHeader(JsonParser jp) throws JsonParseException, IOException
	{
		JsonUtils.nextExpect(jp, JsonToken.START_OBJECT);
		DumpHeader ret = new DumpHeader();
		JsonUtils.nextExpect(jp, "versionMajor");
		ret.setVersionMajor(jp.getIntValue());
		JsonUtils.nextExpect(jp, "versionMinor");
		ret.setVersionMinor(jp.getIntValue());
		JsonUtils.nextExpect(jp, "timestamp");
		ret.setTimestamp(jp.getLongValue());
		return ret;
	}
}
