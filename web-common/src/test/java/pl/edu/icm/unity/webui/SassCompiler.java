/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

public class SassCompiler
{
	public static void main(String[] args) throws Exception
	{
		com.vaadin.sass.SassCompiler.main(new String[] {"src/main/resources/VAADIN/themes/unityThemeValo/styles.scss", 
				"src/main/resources/VAADIN/themes/unityThemeValo/styles.css"});
		com.vaadin.sass.SassCompiler.main(new String[] {"src/main/resources/VAADIN/themes/sidebarThemeValo/styles.scss", 
				"src/main/resources/VAADIN/themes/sidebarThemeValo/styles.css"});

	}
}
