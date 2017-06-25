/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation should perform update of JSON data between subsequent versions.
 * @author K. Benedyczak
 */
public interface Update
{
	/**
	 * Performs update on the input stream of original JSON dump. 
	 * @param is
	 * @return a new Input stream with updated contents, or the argument stream if unchanged.
	 * @throws IOException
	 */
	InputStream update(InputStream is) throws IOException;
}
