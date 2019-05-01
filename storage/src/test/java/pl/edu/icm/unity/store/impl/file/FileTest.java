/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.file;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.store.api.FileDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.impl.AbstractNamedDAOTest;

public class FileTest extends AbstractNamedDAOTest<FileData>
{
	@Autowired
	private FileDAO dao;

	@Before
	public void cleanDB()
	{
		dbCleaner.reset();
	}

	@Override
	protected NamedCRUDDAO<FileData> getDAO()
	{
		return dao;
	}

	@Override
	protected FileData getObject(String id)
	{

		FileData fileData = new FileData(id, "demo".getBytes());
		fileData.setOwnerId("o1");
		fileData.setOwnerType("oType");
		return fileData;
	}

	@Override
	protected FileData mutateObject(FileData src)
	{
		src.setContents("demo2".getBytes());
		src.setOwnerId("o2");
		src.setOwnerType("oType");
		return src;
	}

}
