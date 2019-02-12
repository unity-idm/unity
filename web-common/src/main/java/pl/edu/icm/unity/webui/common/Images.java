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
	certificate	(I.PB + "certificate.png"),
	empty		(I.PB + "empty.png"),

	info		(VaadinIcons.INFO),
	key_o		(VaadinIcons.KEY_O),
	settings	(VaadinIcons.COG_O),
	handshake	(VaadinIcons.HANDSHAKE),
	usertoken	(VaadinIcons.TAGS),
	exit		(VaadinIcons.SIGN_OUT),
	toAdmin		(VaadinIcons.TOOLS),
	toProfile	(VaadinIcons.USER),
	support		(VaadinIcons.LIFEBUOY),

	refresh		(VaadinIcons.REFRESH),
	userMagnifier	(VaadinIcons.SEARCH),
	folder		(VaadinIcons.FOLDER_OPEN_O),
	add		(VaadinIcons.PLUS_CIRCLE_O),
	addIdentity	(VaadinIcons.USER_CARD),
	addEntity	(VaadinIcons.PLUS_CIRCLE_O),
	addFolder	(VaadinIcons.FOLDER_ADD),
	delete		(VaadinIcons.TRASH),
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
	addColumn	(VaadinIcons.PLUS_SQUARE_LEFT_O),
	removeColumn	(VaadinIcons.MINUS_SQUARE_LEFT_O),
	key		(VaadinIcons.KEY),
	attributes	(VaadinIcons.TAGS),
	warn		(VaadinIcons.EXCLAMATION_CIRCLE_O),
	error		(VaadinIcons.EXCLAMATION_CIRCLE),
	transfer	(VaadinIcons.LINK),
	reload		(VaadinIcons.RETWEET),
	download	(VaadinIcons.DOWNLOAD),
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
	menu		(VaadinIcons.MENU),
	remove		(VaadinIcons.CLOSE_CIRCLE_O),
	close_small	(VaadinIcons.CLOSE_SMALL),
	resize		(VaadinIcons.RESIZE_H),
	mobile		(VaadinIcons.MOBILE_RETRO),
	dashboard 	(VaadinIcons.DASHBOARD),
	user		(VaadinIcons.USER),
	question	(VaadinIcons.QUESTION_CIRCLE_O),
	globe		(VaadinIcons.GLOBE),
	family		(VaadinIcons.FAMILY),
	file_tree 	(VaadinIcons.FILE_TREE),
	file_tree_small (VaadinIcons.FILE_TREE_SMALL),
	file_tree_sub 	(VaadinIcons.FILE_TREE_SUB),
	envelope_open	(VaadinIcons.ENVELOPE_OPEN),
	user_check	(VaadinIcons.USER_CHECK),
	star 		(VaadinIcons.STAR),
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
	bullet	        (VaadinIcons.CIRCLE);
	
	private final Resource resource;
	
	private Images(String classpath)
	{
		this.resource = new ThemeResource(classpath);
	}
	
	private Images(Resource resource)
	{
		this.resource = resource;
	}
	
	public Resource getResource()
	{
		return resource;
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
