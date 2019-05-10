/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.files;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.store.api.FileDAO;
import pl.edu.icm.unity.store.rdbms.GenericNamedRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

/**
 * RDBMS storage of {@link FileData}
 * 
 * @author P.Piernik
 */
@Repository(FileRDBMSStore.BEAN)
class FileRDBMSStore extends GenericNamedRDBMSCRUD<FileData, FileBean> implements FileDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public FileRDBMSStore(FileRDBMSSerializer serializer)
	{
		super(FilesMapper.class, serializer, NAME);
	}

	@Override
	public List<Entry<FileData, Date>> getAllWithUpdateTimestamps()
	{
		FilesMapper mapper = SQLTransactionTL.getSql().getMapper(FilesMapper.class);
		List<FileBean> allInDB = mapper.getAll();	
		List<Map.Entry<FileData, Date>> ret = new ArrayList<>(allInDB.size());
		for (FileBean raw: allInDB)
			ret.add(new AbstractMap.SimpleEntry<FileData, Date>(
					jsonSerializer.fromDB(raw), raw.getLastUpdate()));
		return ret;
	}

	@Override
	public List<Entry<String, Date>> getAllNamesWithUpdateTimestamps()
	{
		FilesMapper mapper = SQLTransactionTL.getSql().getMapper(FilesMapper.class);
		List<FileBean> allInDB = mapper.getAll();	
		List<Map.Entry<String, Date>> ret = new ArrayList<>(allInDB.size());
		for (FileBean raw: allInDB)
			ret.add(new AbstractMap.SimpleEntry<String, Date>(
					raw.getName(), raw.getLastUpdate()));
		return ret;
	}

	@Override
	public Date getUpdateTimestamp(String name)
	{
		FilesMapper mapper = SQLTransactionTL.getSql().getMapper(FilesMapper.class);
		FileBean raw = mapper.getByName(name);
		if (raw == null)
			throw new IllegalArgumentException("There is no [" + name + "] " + "file");
		
		return raw.getLastUpdate();
	}

	@Override
	public void updateTS(String id)
	{
		FilesMapper mapper = SQLTransactionTL.getSql().getMapper(FilesMapper.class);
		FileBean raw = mapper.getByName(id);
		if (raw == null)
			throw new IllegalArgumentException("There is no [" + id + "] " + "file");
		raw.setLastUpdate(new Date());
		mapper.updateByKey(raw);
		
	}

	@Override
	public long createWithTS(FileData newValue, Date updatTS)
	{
		FileData ret = new FileData(newValue.name, newValue.contents, updatTS, newValue.ownerType, newValue.ownerId);
		return create(ret);
	}
}
