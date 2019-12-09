/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.files;

import java.net.URI;

import pl.edu.icm.unity.base.file.FileData;

public interface URIAccessService
{
	public static final String UNITY_FILE_URI_SCHEMA = "unity.internal";
	
	FileData readURI(URI uri);

	FileData readURI(URI uri, String customTruststore);
	
	FileData readImageURI(URI uri, String themeName);
}
