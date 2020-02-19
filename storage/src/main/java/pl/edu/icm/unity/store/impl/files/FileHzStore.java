/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.files;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.store.api.FileDAO;
import pl.edu.icm.unity.store.types.UpdateFlag;

@Repository(FileHzStore.STORE_ID)
public class FileHzStore implements FileDAO
{
	public static final String STORE_ID = DAO_ID + "hz";

	@Override
	public List<Entry<FileData, Date>> getAllWithUpdateTimestamps()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Entry<String, Date>> getAllNamesWithUpdateTimestamps()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getUpdateTimestamp(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateTS(String id)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public long createWithTS(FileData newValue, Date updatTS)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void delete(String id)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateByNameControlled(String current, FileData newValue, EnumSet<UpdateFlag> flags)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean exists(String id)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<String, FileData> getAllAsMap()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FileData get(String id)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getKeyForName(String id)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Set<String> getAllNames()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long create(FileData obj)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void createWithId(long id, FileData obj)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateByKey(long id, FileData obj)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteByKey(long id)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteAll()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public FileData getByKey(long id)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FileData> getAll()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getCount()
	{
		// TODO Auto-generated method stub
		return 0;
	}


}
