/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.files;

import pl.edu.icm.unity.base.file.FileData;

import java.net.URI;
import java.time.Duration;

public interface URIAccessService
{
	public static final String UNITY_FILE_URI_SCHEMA = "unity.internal";
	
	FileData readURI(URI uri);

	FileData readURI(URI uri, String customTruststore);

	RemoteFileData readURL(URI uri, String customTruststore, Duration connectionAndSocketReadTimeout, int retriesNumber);
	
	FileData readImageURI(URI uri, String themeName);
}
