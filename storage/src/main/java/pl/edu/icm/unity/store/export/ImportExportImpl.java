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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.ImportExport;

/**
 * Import/export functionality. 
 * @author K. Benedyczak
 */
@Component
public class ImportExportImpl implements ImportExport
{
	public static final int VERSION = 3;

	private ObjectMapper objectMapper;
	private DumpUpdater updater;
	private List<AbstractIEBase<?>> implementations;

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
	}

	@Override
	public void store(OutputStream os) throws IOException
	{
		JsonFactory jsonF = new JsonFactory(objectMapper);
		JsonGenerator jg = jsonF.createGenerator(os, JsonEncoding.UTF8);
		jg.useDefaultPrettyPrinter();
		
		jg.writeStartObject();
		
		jg.writeNumberField("versionMajor", VERSION);
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
			throw new IOException("Only InputStreams supporting mark()&reset() are allowed to load from.");
		
		JsonFactory jsonF = new JsonFactory(objectMapper);
		JsonParser jp = jsonF.createParser(is);
		JsonUtils.nextExpect(jp, JsonToken.START_OBJECT);
		
		DumpHeader header = loadHeader(jp);

		is.mark(-1);
		updater.update(is, header);
		is.reset();
		
		JsonUtils.nextExpect(jp, "contents");
		
		for (AbstractIEBase<?> impl: implementations)
		{
			JsonUtils.nextExpect(jp, impl.getStoreKey());
			impl.deserialize(jp);
		}
		jp.close();
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
}
