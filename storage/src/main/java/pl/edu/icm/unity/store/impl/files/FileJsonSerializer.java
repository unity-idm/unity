/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.files;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;

/**
 * Serializes {@link FileData} to/from DB form.
 * @author P.Piernik
 */
@Component
class FileJsonSerializer implements JsonSerializerForKryo<FileData>
{
	@Autowired
	private ObjectMapper mapper;


	@Override
	public FileData fromJson(ObjectNode src)
	{
		return mapper.convertValue(src, FileData.class);
	}

	@Override
	public ObjectNode toJson(FileData src)
	{
		return mapper.convertValue(src, ObjectNode.class);
	}

	@Override
	public Class<FileData> getClazz()
	{
		return FileData.class;
	}
}
