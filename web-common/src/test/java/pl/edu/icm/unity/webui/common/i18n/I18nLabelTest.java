/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import org.junit.Assert;
import org.junit.Test;

public class I18nLabelTest
{
	@Test
	public void breakLinesWorks()
	{
		String ret = I18nLabel.breakLines("123 567 901234567890", 10);
		Assert.assertEquals("123 567 \n9012345678\n90", ret);

		ret = I18nLabel.breakLines("123 567 90", 10);
		Assert.assertEquals("123 567 90", ret);

		ret = I18nLabel.breakLines("12345678901", 10);
		Assert.assertEquals("1234567890\n1", ret);
	}

	@Test
	public void breakLinesWorksWithEnters()
	{
		String ret = I18nLabel.breakLines("1234567890\n1234567890\n", 10);
		Assert.assertEquals("1234567890\n1234567890\n", ret);
	}


	@Test
	public void breakLinesWorksWithEntersMixed()
	{
		String ret = I18nLabel.breakLines("1234\n123 567890 1234567890\n1234\n\n123 123 123 123 123", 10);
		Assert.assertEquals("1234\n123 \n567890 \n1234567890\n1234\n\n123 123 \n123 123 \n123", ret);
	}
}
