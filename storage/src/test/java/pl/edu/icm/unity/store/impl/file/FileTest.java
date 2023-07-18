/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.file;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.store.api.FileDAO;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.impl.AbstractNamedDAOTest;

public class FileTest extends AbstractNamedDAOTest<FileData>
{
	@Autowired
	private FileDAO dao;

	@BeforeEach
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
	}

	@Override
	protected NamedCRUDDAOWithTS<FileData> getDAO()
	{
		return dao;
	}

	@Override
	protected FileData getObject(String id)
	{

		FileData fileData = new FileData(id, "demo".getBytes(), new Date(), "oType", "o1");
		return fileData;
	}

	@Override
	protected FileData mutateObject(FileData src)
	{
		FileData newV = new FileData(src.getName(), "demo2".getBytes(), new Date(), "oType2", "o2");
		return newV;
	}

}
