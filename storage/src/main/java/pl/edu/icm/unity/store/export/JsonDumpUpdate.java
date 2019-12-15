/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation should perform update of JSON data between subsequent versions.
 */
public interface JsonDumpUpdate
{
	/**
	 * Performs update on the input stream of original JSON dump. 
	 * @return a new Input stream with updated contents, or the argument stream if unchanged.
	 */
	InputStream update(InputStream is) throws IOException;
	
	/**
	 * @return version on which this updater should be executed. It is assumed that after update 
	 * dump in be in the version +1.
	 */
	int getUpdatedVersion();
}
