/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides feature to export and import database dumps to a file. 
 * <p>
 * The implementation must allows for updating imported contents if it was stored by an older Unity version. 
 * @author K. Benedyczak
 */
public interface ImportExport
{
	void load(InputStream is) throws IOException;
	void store(OutputStream os) throws IOException;
	void storeWithVersion(OutputStream os, int version) throws IOException;
}
