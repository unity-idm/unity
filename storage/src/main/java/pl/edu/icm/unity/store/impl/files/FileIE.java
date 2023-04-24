/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.files;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.store.api.FileDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;

/**
 * Handles import/export of file.
 * @author P.Piernik
 */
@Component
public class FileIE extends AbstractIEBase<FileData>
{
	public static final String FILES_OBJECT_TYPE = "files";
	
	private FileDAO dao;
	private FileJsonSerializer serializer;
	
	@Autowired
	public FileIE(FileDAO dao, FileJsonSerializer serializer, ObjectMapper objectMapper)
	{
		super(7, FILES_OBJECT_TYPE, objectMapper);
		this.dao = dao;
		this.serializer = serializer;
	}

	@Override
	protected List<FileData> getAllToExport()
	{
		return dao.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(FileData exportedObj)
	{
		return serializer.toJson(exportedObj);
	}

	@Override
	protected void createSingle(FileData toCreate)
	{
		dao.create(toCreate);
	}

	@Override
	protected FileData fromJsonSingle(ObjectNode src)
	{
		return serializer.fromJson(src);
	}
}



