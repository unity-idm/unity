/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.files;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.store.api.FileDAO;
import pl.edu.icm.unity.store.rdbms.GenericNamedRDBMSCRUD;

/**
 * RDBMS storage of {@link FileData}
 * 
 * @author P.Piernik
 */
@Repository(FileRDBMSStore.BEAN)
public class FileRDBMSStore extends GenericNamedRDBMSCRUD<FileData, FileBean> implements FileDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public FileRDBMSStore(FileRDBMSSerializer serializer)
	{
		super(FilesMapper.class, serializer, NAME);
	}
}
