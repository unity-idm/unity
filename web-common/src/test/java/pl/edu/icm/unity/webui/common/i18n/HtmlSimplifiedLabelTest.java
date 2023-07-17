/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabel;

public class HtmlSimplifiedLabelTest
{
	@Test
	public void prohibitedCharsEscaped()
	{
		HtmlSimplifiedLabel l = new HtmlSimplifiedLabel();
		l.setValue("q<i>italics</I><p>escapedpara</p><br><hr>");
		
		assertEquals("q<i>italics</I>&lt;p&gt;escapedpara&lt;/p&gt;<br><hr>", l.getValue());
	}
}
