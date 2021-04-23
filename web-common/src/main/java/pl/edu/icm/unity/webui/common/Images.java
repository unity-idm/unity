/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FontIcon;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;

/**
 * Allows to easily get image resources.
 * @author K. Benedyczak
 */
public enum Images
{
	logo		(I.PB + "logo.png"),
	logoSmall	(I.PB + "logo-hand.png"),
	loader		(I.PB + "loader.gif"),
	password	(I.PB + "password.png"),
	mobile_sms	(I.PB + "mobile-sms.png"),
	otp		(I.PB + "mobile-sms.png"),
	certificate	(I.PB + "certificate.png"),
	empty		(I.PB + "empty.png"),

	info		(VaadinIcons.INFO),
	info_circle	(VaadinIcons.INFO_CIRCLE_O),
	key_o		(VaadinIcons.KEY_O),
	settings	(VaadinIcons.COG_O),
	handshake	(VaadinIcons.HANDSHAKE),
	usertoken	(VaadinIcons.TAGS),
	exit		(VaadinIcons.SIGN_OUT),
	sign_in		(VaadinIcons.SIGN_IN),
	toAdmin		(VaadinIcons.TOOLS),
	toProfile	(VaadinIcons.USER),
	support		(VaadinIcons.LIFEBUOY),

	refresh		(VaadinIcons.REFRESH),
	userMagnifier	(VaadinIcons.SEARCH),
	folder_open_o	(VaadinIcons.FOLDER_OPEN_O),
	folder_close_o	(VaadinIcons.FOLDER_O),
	folder_open     (VaadinIcons.FOLDER_OPEN),
	folder_close    (VaadinIcons.FOLDER),
	add		(VaadinIcons.PLUS_CIRCLE_O),
	addIdentity	(VaadinIcons.USER_CARD),
	addEntity	(VaadinIcons.PLUS_CIRCLE_O),
	addFolder	(VaadinIcons.FOLDER_ADD),
	delete		(VaadinIcons.TRASH),
	recycle 	(VaadinIcons.RECYCLE),
	removeFromGroup (VaadinIcons.BAN),
	reject 		(VaadinIcons.BAN),
	undeploy	(VaadinIcons.BAN),
	deleteFolder	(VaadinIcons.FOLDER_REMOVE),
	deleteEntity	(VaadinIcons.TRASH),
	deleteIdentity	(VaadinIcons.TRASH),
	edit		(VaadinIcons.EDIT),
	copy		(VaadinIcons.COPY_O),
	editUser	(VaadinIcons.POWER_OFF),
	ok		(VaadinIcons.CHECK_CIRCLE_O),
	ok_small	(VaadinIcons.CHEVRON_DOWN_SMALL),
	save		(VaadinIcons.DISC),
	export		(VaadinIcons.UPLOAD),
	trashBin	(VaadinIcons.TRASH),
	addFilter	(VaadinIcons.FUNNEL),
	noAuthzGrp	(VaadinIcons.LOCK),
	collapse	(VaadinIcons.FOLDER_O),
	expand		(VaadinIcons.FOLDER_OPEN_O),
	embed		(VaadinIcons.EXPAND_SQUARE),
	addColumn	(VaadinIcons.PLUS_SQUARE_LEFT_O),
	removeColumn	(VaadinIcons.MINUS_SQUARE_LEFT_O),
	key		(VaadinIcons.KEY),
	attributes	(VaadinIcons.TAGS),
	warn		(VaadinIcons.EXCLAMATION_CIRCLE_O),
	error		(VaadinIcons.EXCLAMATION_CIRCLE),
	transfer	(VaadinIcons.LINK),
	reload		(VaadinIcons.RETWEET),
	download	(VaadinIcons.DOWNLOAD),
	upload		(VaadinIcons.UPLOAD),
	upArrow		(VaadinIcons.ANGLE_UP),
	topArrow	(VaadinIcons.ANGLE_DOUBLE_UP),
	downArrow	(VaadinIcons.ANGLE_DOWN),
	bottomArrow	(VaadinIcons.ANGLE_DOUBLE_DOWN),
	rightArrow 	(VaadinIcons.ANGLE_RIGHT),
	rightDoubleArrow (VaadinIcons.ANGLE_DOUBLE_RIGHT),
	leftDoubleArrow (VaadinIcons.ANGLE_DOUBLE_LEFT),
	wizard		(VaadinIcons.MAGIC),
	dryrun		(VaadinIcons.COG_O),
	play		(VaadinIcons.PLAY),
	messageSend	(VaadinIcons.ENVELOPE_O),
	pause		(VaadinIcons.PAUSE),
	plFlag		(I.P + "16-flags/pl.png"),
	enFlag		(I.P + "16-flags/en.png"),
	deFlag		(I.P + "16-flags/de.png"),
	nbFlag		(I.P + "16-flags/no.png"),
	frFlag		(I.P + "16-flags/fr.png"),
	menu		(VaadinIcons.MENU),
	remove		(VaadinIcons.CLOSE_CIRCLE_O),
	close_small	(VaadinIcons.CLOSE_SMALL),
	resize		(VaadinIcons.RESIZE_H),
	mobile		(VaadinIcons.MOBILE_RETRO),
	dashboard 	(VaadinIcons.DASHBOARD),
	user		(VaadinIcons.USER),
	question	(VaadinIcons.QUESTION_CIRCLE_O),
	globe		(VaadinIcons.GLOBE),
	globe_wire		(VaadinIcons.GLOBE_WIRE),
	family		(VaadinIcons.FAMILY),
	file_tree 	(VaadinIcons.FILE_TREE),
	file_tree_small (VaadinIcons.FILE_TREE_SMALL),
	file_tree_sub 	(VaadinIcons.FILE_TREE_SUB),
	file_zip	(VaadinIcons.FILE_ZIP),
	envelope_open	(VaadinIcons.ENVELOPE_OPEN),
	envelopes_open	(VaadinIcons.ENVELOPES_O),
	user_check	(VaadinIcons.USER_CHECK),
	star 		(VaadinIcons.STAR),
	star_open 	(VaadinIcons.STAR_O),
	trending_down 	(VaadinIcons.TRENDIND_DOWN),
	trending_up 	(VaadinIcons.TRENDING_UP),
	padlock_lock	(VaadinIcons.LOCK),
	padlock_unlock	(VaadinIcons.UNLOCK),
	pencil		(VaadinIcons.PENCIL),
	forward		(VaadinIcons.FORWARD),
	external_link 	(VaadinIcons.EXTERNAL_LINK),
	trash		(VaadinIcons.TRASH),
	envelope	(VaadinIcons.ENVELOPE),
	records	        (VaadinIcons.RECORDS),
	bullet	        (VaadinIcons.CIRCLE),
	bullets	        (VaadinIcons.BULLETS),
	file_add	(VaadinIcons.FILE_ADD),
	picture		(VaadinIcons.PICTURE),
	caret_right	(VaadinIcons.CARET_RIGHT),
	caret_down	(VaadinIcons.CARET_DOWN),
	group		(VaadinIcons.GROUP),
	form		(VaadinIcons.FORM),
	user_card	(VaadinIcons.USER_CARD),
	taxi		(VaadinIcons.TAXI),
	server		(VaadinIcons.SERVER),
	lock		(VaadinIcons.LOCK),
	optiona_a	(VaadinIcons.OPTION_A),
	grid		(VaadinIcons.GRID_BIG),
	tags		(VaadinIcons.TAGS),
	clipboard_user	(VaadinIcons.CLIPBOARD_USER),
	archives	(VaadinIcons.ARCHIVES),
	archive		(VaadinIcons.ARCHIVE),
	calendar_user   (VaadinIcons.CALENDAR_USER),
	cogs		(VaadinIcons.COGS),
	diploma		(VaadinIcons.DIPLOMA),
	tools		(VaadinIcons.TOOLS),
	cloud_download	(VaadinIcons.CLOUD_DOWNLOAD_O),
	text		(VaadinIcons.TEXT_LABEL),
	header		(VaadinIcons.HEADER),
	grid_v		(VaadinIcons.GRID_V),
	combobox	(VaadinIcons.COMBOBOX), 
	cubes		(VaadinIcons.CUBES),
	cube		(VaadinIcons.CUBE),
	money		(VaadinIcons.MONEY),
	flag_final	(VaadinIcons.FLAG_CHECKERED),	
	coin_piles	(VaadinIcons.COIN_PILES),
	check_square	(VaadinIcons.CHECK_SQUARE_O),
	text_label	(VaadinIcons.TEXT_LABEL),
	eraser		(VaadinIcons.ERASER),
	check		(VaadinIcons.CHECK),
	list_select	(VaadinIcons.LIST_SELECT),
	workplace	(VaadinIcons.WORKPLACE),
	home		(VaadinIcons.HOME);
	
	private final Resource resource;
	private final String path;
	
	private Images(String classpath)
	{
		this.resource = new ThemeResource(classpath);
		this.path = classpath;
	}
	
	private Images(Resource resource)
	{
		this.resource = resource;
		this.path = null;
	}
	
	public Resource getResource()
	{
		return resource;
	}
	
	public String getPath()
	{
		if (!(resource instanceof FontIcon))
		{
			return path;
		}
		throw new IllegalArgumentException("Icon is not classpath icon and do not supprot getPath()");
	}
	
	public String getHtml()
	{
		if (resource instanceof FontIcon)
			return ((FontIcon) resource).getHtml();
		throw new IllegalArgumentException("Icon is not font icon and do not supprot getHtml()");
	}

	public static Resource getFlagForLocale(String localeCode)
	{
		switch (localeCode)
		{
		case "en":
			return enFlag.getResource();
		case "pl":
			return plFlag.getResource();
		case "de":
			return deFlag.getResource();
		case "nb":
			return nbFlag.getResource();
		case "fr":
			return frFlag.getResource();
		}
		return null;
	}
	
	/**
	 * Trick - otherwise we won't be able to use P in enum constructor arguments
	 * @author K. Benedyczak
	 */
	private static interface I
	{
		public static final String P = "../common/img/standard/";
		public static final String PB = "../common/img/other/";
	}
}
