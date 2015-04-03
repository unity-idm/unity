/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.themes.ValoTheme;

/**
 * General purpose styles defined for VAADIN components
 * @author K. Benedyczak
 */
public enum Styles
{
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
	textEndpointName("u-textEndpointHeading"),
	textSubHeading("u-textHeading2"),
	toolbarButton("u-toolbarButton"),
	verticalLine("u-verticalLine"),
	horizontalLine("u-horizontalLine"),
	header("u-header"),
	selectedButton("u-selectedButton"),
	verticalPadding10("u-verticalPadding10"),
	horizontalPadding10("u-horizontalPadding10"),
	verticalPadding16("u-verticalPadding16"),
	horizontalMargins10("u-horizontalMargins10"),
	verticalMargins10("u-verticalMargins10"),
	horizontalMargins6("u-horizontalMargins6"),
	verticalMargins6("u-verticalMargins6"),
	minHeight300("u-minHeight300"),
	contentPadRight20("u-padRight20"), //pads the *contents* of the container 20px on right
	width100("u-width100"),
	width50("u-width50"),
	height100("u-height100"),
	height50("u-height50"),
	maxHeight200("u-maxHeight200"),
	maxHeight100("u-maxHeight100"),
	maxHeight50("u-maxHeight50"),
	greenBackground("u-green-bg"),
	grayBackground("u-gray-bg"),
	redBackground("u-red-bg"),
	smallMargin("u-smallMargin"),
	smallSpacing("u-smallSpacing"),
	tinySpacing("u-tinySpacing"),
	smallFormSpacing("u-smallFormSpacing"),
	negativeMargin5("u-negativeMargin5"),
	captionBold("bold"),
	hidden("u-hidden"),
	largeTabsheet("u-bigTabsheet"),
	
	//Valo
	vPanelLight(ValoTheme.PANEL_BORDERLESS),
	vBorderLess(ValoTheme.TEXTAREA_BORDERLESS),
	vButtonSmall(ValoTheme.BUTTON_SMALL),
	vTabsheetMinimal(ValoTheme.TABSHEET_COMPACT_TABBAR),
	vTabsheetFramed(ValoTheme.TABSHEET_FRAMED),
	vLabelSmall(ValoTheme.LABEL_SMALL),
	vLabelLarge(ValoTheme.LABEL_LARGE),
	vLabelH1(ValoTheme.LABEL_H1),
	vButtonLink(ValoTheme.BUTTON_LINK),
	vButtonLinkV(ValoTheme.BUTTON_LINK),
	vSmall(ValoTheme.TEXTFIELD_SMALL),
	vTiny(ValoTheme.TEXTFIELD_TINY),
	vTableNoHorizontalLines(ValoTheme.TABLE_NO_HORIZONTAL_LINES),
	vComboSmall(ValoTheme.COMBOBOX_SMALL);
	
	
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
