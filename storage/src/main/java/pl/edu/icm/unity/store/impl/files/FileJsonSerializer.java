/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.files;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.file.FileData;

/**
 * Serializes {@link FileData} to/from DB form.
 * @author P.Piernik
 */
@Component
class FileJsonSerializer
{
	private final ObjectMapper mapper;

	FileJsonSerializer(ObjectMapper mapper)
	{
		this.mapper = mapper;
	}

	FileData fromJson(ObjectNode src)
	{
		return mapper.convertValue(src, FileData.class);
	}

	ObjectNode toJson(FileData src)
	{
		return mapper.convertValue(src, ObjectNode.class);
	}
}
