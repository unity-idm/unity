/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.files;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;

/**
 * Serializes {@link FileData} to/from RDBMS {@link FileBean} form.
 * @author P.Piernik
 */
@Component
public class FileRDBMSSerializer implements RDBMSObjectSerializer<FileData, FileBean>
{
	@Override
	public FileBean toDB(FileData object)
	{
		FileBean ret = new FileBean(object.getName(), 
				object.getOwnerType(), object.getOwnerId(), object.getContents());
		return ret;
	}

	@Override
	public FileData fromDB(FileBean bean)
	{
		FileData fileData = new FileData(bean.getName(), bean.getContents());
		fileData.setOwnerType(bean.getOwnerType());
		fileData.setOwnerId(bean.getOwnerId());
		return fileData;
	}
}
