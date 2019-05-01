/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.store.api;

import pl.edu.icm.unity.base.file.FileData;

/**
 * Files DAO
 * @author P.Piernik
 *
 */
public interface FileDAO extends NamedCRUDDAO<FileData>
{
	String DAO_ID = "FileDAO";
	String NAME = "file";
}
