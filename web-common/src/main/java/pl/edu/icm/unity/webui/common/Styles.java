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
	emphasized("u-emphasized"),
	bold("u-bold"),
	immutableAttribute("u-immutableAttribute"),
	readOnlyTableElement("u-readOnlyTableElement"),
	captionBold("bold"),
	trueConditionBackground("u-trueCondition-bg"),
	falseConditionBackground("u-falseCondition-bg"),
	background("u-backgroud"),
	errorBackground("u-error-bg"),
	negativeBottomMarginSmall("u-negativeBottomMarginSmall"),
	negativeTopMargin("u-negativeTopMargin"),
	verticalPaddingSmall("u-verticalPaddingSmall"),
	smallMargins("u-smallMargins"),
	iconError("u-error-icon"),
	
	bigTabs("u-bigTabs"),
	bigTab("u-bigTab"),
	bigTabSelected("u-bigTabSelected"),
	textCenter("u-textCenter"),
	formSection("u-formSection"),
	messageBox("u-messageBox"),
	error("u-error"),
	success("u-success"),
	textTitle("u-textTitle"),
	textInfoTooltip("u-textInfoTooltip"),
	textLarge("u-textLarge"),
	textXLarge("u-textXLarge"),
	textEndpointName("u-textEndpointHeading"),
	toolbarButton("u-toolbarButton"),
	toolbar("u-toolbar"),
	verticalBar("u-verticalBar"),
	horizontalBar("u-horizontalBar"),
	horizontalBarBold("u-horizontalBarBold"),
	header("u-header"),
	selectedButton("u-selectedButton"),
	minHeightAuthn("u-minHeightAuthenticator"),
	smallMargin("u-smallMargin"),
	smallSpacing("u-smallSpacing"),
	tinySpacing("u-tinySpacing"),
	smallFormSpacing("u-smallFormSpacing"),
	hidden("u-hidden"),
	largeTabsheet("u-bigTabsheet"),
	centeredPanel("u-centeredPanel"),
	visibleScroll("u-visiblescroll"),
	verticalAlignmentMiddle("u-vAlignMiddle"),
	floatRight("u-floatRight"),
	horizontalMarginSmall("u-hMarginSmall"),
	rightMargin("u-rightMargin"),
	leftMargin("u-leftMargin"),
	leftMarginSmall("u-leftMarginSmall"),
	passwordQuality("u-passwordQuality"),
	nonCompactTopMargin("u-passwordQuality-nonCompactTopMargin"),
	bottomMargin("u-bottomMargin"),
	margin("u-margin"),
	idpTile("u-idptile"),
	indent("u-indent"),
	link("u-link"),
	hamburgerMenu("u-hamburgerMenu"),
	gridNoHorizontalLines("u-noGridHorizontalLines"),
	denseTreeGrid("u-denseTreeGrid"),
	gridWithAction("u-gridWithAction"),
	smallGrid("u-smallGrid"),
	border("u-border"),
	smallIconLabel("u-smallIconLabel"),
	mediumIcon("u-mediumIcon"),
	largeIcon("u-largeIcon"),
	veryLargeIcon("u-veryLargeIcon"),
	signInButton("u-signInButton"),
	signUpButton("u-signUpButton"),
	externalSignInButton("u-externalSignInButton"),
	externalGridSignInButton("u-externalGridSignInButton"),
	greenProgressBar("u-greenProgressBar"),
	redProgressBar("u-redProgressBar"),
	RegistrationLink("registrationLink"),
	indentComboBox("u-indentComboBox"),
	wordWrap("u-wordWrap"),
	sectionTitle("u-sectionTitle"),
	showHideButton("u-showHideButton"),
	imagePreview("u-imagePreview"),
	disabledButton("u-button-disabled "),
	narrowTable("u-narrowTable"),
	borderBold("u-border-bold"),
	borderBoldGreen("u-border-bold-green"),
	readOnlyComboBox("u-readOnlyComboBox"),	
	indentSmall("u-indentSmall"),	
	buttonAction("u-button-action"),
	fixedWidthCaptions("u-fixedWidthCaptions"),
	fixedWidthShortCaptions("u-fixedWidthCaptionsSmall"),
	gridLinkButton("u-button-grid-link"),
	viewTitle("u-viewTitle"),
	viewSubtitle("u-viewSubtitle"),
	policyAgreementItem("u-policyagreement-item"),
	dragButton("u-dragButton"),
	varPickerButton("u-varPickerButton"),
	
	//Sidebar
	sidebar("u-sidebar"),
	leftMenu("u-leftMenu"),
	topRightMenu("u-topRightMenu"),
	headerBar("u-headerBar"),
	contentBox("u-contentBox"),
	rootContent("u-rootContent"),
	menuButton("u-menuButton"),
	menuButtonClickable("u-clickable"),
	menuButtonActive("u-active"),
	menuLabel("u-menuLabel"),
	menuCombo("u-menuCombo"),
	subMenu("u-subMenu"),
	subMenuOpen("u-open"),
	tooltip("u-toolTip"),
	breadcrumbs("u-breadcrumbs"),
	viewHeader("u-viewHeader"),
	contentBackgroud("u-content-backgroud"),
	
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
	vButtonBorderless(ValoTheme.BUTTON_BORDERLESS),
	vButtonPrimary(ValoTheme.BUTTON_PRIMARY),
	vSmall(ValoTheme.TEXTFIELD_SMALL),
	vTiny(ValoTheme.TEXTFIELD_TINY),
	vTableNoHorizontalLines(ValoTheme.TABLE_NO_HORIZONTAL_LINES),
	vComboSmall(ValoTheme.COMBOBOX_SMALL),
	vDropLayout("drop-layout"),	
	vGroupBrowser("v-tree8"),
	vPanelBorderless(ValoTheme.PANEL_BORDERLESS);
	
	
	
	
	/**
	 * Number of columns for wider then regular text fields.
	 */
	public static final int WIDE_TEXT_FIELD = 40;
	
	private String value;
	
	private Styles(String value)
	{
		this.value = value;
	}
	
	@Override
	public String toString()
	{
		return value;
	}
	
	public static String getFlagBgStyleForLocale(String localeCode)
	{
		switch (localeCode)
		{
		case "en":
		case "fr":
		case "pl":
		case "de":
		case "nb":
			return "u-flag-bg-" + localeCode;
		}
		return null;
	}
}
