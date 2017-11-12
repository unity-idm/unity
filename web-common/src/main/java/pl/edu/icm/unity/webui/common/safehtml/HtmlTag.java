/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.safehtml;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.webui.common.Styles;

/**
 * HTML label displaying commonly used simple steering sequences as hr.
 *   
 * @author K. Benedyczak
 */
public class HtmlTag
{
	public static Label horizontalLine()
	{
		Label ret = new Label();
		ret.setStyleName(Styles.horizontalBar.toString());
		return ret;
	}

	public static Label br()
	{
		return new Label("<br>", ContentMode.HTML);
	}
	
	/**
	 * @param length length in em
	 * @return
	 */
	public static Label hspaceEm(int length)
	{
		return new Label("<div style=\"width:" + length + "em;float:left;overflow:hidden;height:1px;\"/>", 
				ContentMode.HTML);
	}
}
