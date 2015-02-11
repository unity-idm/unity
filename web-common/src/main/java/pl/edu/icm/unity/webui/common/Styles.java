/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.themes.Reindeer;

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
	success("u-success"),
	textLarge("u-textLarge"),
	textXLarge("u-textXLarge"),
	textHeading("u-textHeading"),
	toolbarButton("u-toolbarButton"),
	verticalLine("u-verticalLine"),
	horizontalLine("u-horizontalLine"),
	header("u-header"),
	selectedButton("u-selectedButton"),
	padding10("u-padding10"),
	horizontalMargins10("u-horizontalMargins10"),
	verticalMargins10("u-verticalMargins10"),
	horizontalMargins6("u-horizontalMargins6"),
	verticalMargins6("u-verticalMargins6"),
	maxHeight300("u-maxHeight300"),
	contentPadRight20("u-padRight20"), //pads the *contents* of the container 20px on right
	width100("u-width100"),
	width50("u-width50"),
	height100("u-height100"),
	height50("u-height50"),
	greenBackground("u-green-bg"),
	grayBackground("u-gray-bg"),
	redBackground("u-red-bg"),
	smallMargin("u-smallMargin"),
	smallSpacing("u-smallSpacing"),
	negativeMargin5("u-negativeMargin5"),
	captionBold("bold"),
	
	//reindeer
	vPanelLight(Reindeer.PANEL_LIGHT),
	vButtonSmall(Reindeer.BUTTON_SMALL),
	vTabsheetMinimal(Reindeer.TABSHEET_MINIMAL),
	vLabelSmall(Reindeer.LABEL_SMALL),
	vLabelH2(Reindeer.LABEL_H2),
	vLabelH1(Reindeer.LABEL_H1),
	vButtonLink(Reindeer.BUTTON_LINK),
	vSplitPanelSmall(Reindeer.SPLITPANEL_SMALL),
	vTextfieldSmall(Reindeer.TEXTFIELD_SMALL);
	
	
	private String value;
	
	private Styles(String value)
	{
		this.value = value;
	}
	
	public String toString()
	{
		return value;
	}
	
	public static String getFlagBgStyleForLocale(String localeCode)
	{
		switch (localeCode)
		{
		case "en":
		case "pl":
		case "de":
			return "u-flag-bg-" + localeCode;
		}
		return null;
	}
}
