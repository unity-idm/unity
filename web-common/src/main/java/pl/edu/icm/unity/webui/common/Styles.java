/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

/**
 * General purpose styles defined for VAADIN components
 * @author K. Benedyczak
 */
public enum Styles
{
	authnSetSelect("authnSetSelect"),
	bigTabs("u-bigTabs"),
	bigTab("u-bigTab"),
	bigTabSelected("u-bigTabSelected"),
	textCenter("u-textCenter"),
	italic("u-italic"),
	bold("u-bold"),
	formSection("u-formSection"),
	gray("u-gray"),
	messageBox("u-messageBox"),
	error("u-error"),
	textLarge("u-textLarge"),
	textXLarge("u-textXLarge"),
	toolbarButton("u-toolbarButton"),
	verticalLine("u-verticalLine"),
	horizontalLine("u-horizontalLine"),
	header("u-header"),
	selectedButton("u-selectedButton"),
	horizontalMargins10("u-horizontalMargins10"),
	verticalMargins10("u-verticalMargins10"),
	captionBold("bold");
	
	
	private String value;
	
	private Styles(String value)
	{
		this.value = value;
	}
	
	public String toString()
	{
		return value;
	}
}
