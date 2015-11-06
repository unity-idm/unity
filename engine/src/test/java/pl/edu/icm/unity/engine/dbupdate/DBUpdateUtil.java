/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.dbupdate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class DBUpdateUtil
{
	public static void installTestDB(String version) throws IOException
	{
		File target = new File("target/test-classes/dbUpdate/to" + version + "/data/");
		
		FileUtils.deleteDirectory(target);
		
		assertThat(target.exists(), is(false));
		
		FileUtils.copyDirectory(new File("src/test/resources/dbUpdate/to" + version + "/data"), target);
	}
}
