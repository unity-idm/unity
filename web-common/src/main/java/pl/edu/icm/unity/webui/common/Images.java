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
	loader		(I.PB + "loader.gif"),
	password	(I.PB + "password.png"),
	certificate	(I.PB + "certificate.png"),
	empty		(I.PB + "empty.png"),
	 
	info64		(I.PH + "64/bullet_info.png"),
	key64		(I.PH + "64/key.png"),
	settings64	(I.PH + "64/settings.png"),
	usertoken64	(I.PH + "64/user_token.png"),

	stderror64	(I.P + "64/error.png"),
	stdwarn64	(I.P + "64/warning.png"),
	stdinfo64	(I.P + "64/info.png"),
	
	exit32		(I.P + "32/exit.png"),
	error32		(I.P + "32/error.png"),
	warn32		(VaadinIcons.WARNING),
	info32		(I.P + "32/information.png"),
	toAdmin32	(I.P + "32/manager.png"),
	toProfile32	(I.P + "32/personal.png"),
	support32	(I.P + "32/support.png"),
	ok32		(I.P + "32/ok.png"),

	
	refresh		(VaadinIcons.REFRESH),
	userMagnifier	(VaadinIcons.SEARCH),
	folder		(VaadinIcons.FOLDER_OPEN_O),
	add		(VaadinIcons.PLUS_CIRCLE_O),
	addIdentity	(VaadinIcons.USER_CARD),
	addEntity	(VaadinIcons.PLUS_CIRCLE_O),
	addFolder	(VaadinIcons.FOLDER_ADD),
	delete		(VaadinIcons.MINUS_CIRCLE_O),
	removeFromGroup (VaadinIcons.BAN),
	undeploy	(VaadinIcons.BAN),
	deleteFolder	(VaadinIcons.FOLDER_REMOVE),
	deleteEntity	(VaadinIcons.MINUS_CIRCLE_O),
	deleteIdentity	(VaadinIcons.MINUS_CIRCLE_O),
	edit		(VaadinIcons.EDIT),
	copy		(VaadinIcons.COPY_O),
	editUser	(VaadinIcons.POWER_OFF),
	ok		(VaadinIcons.CHECK_CIRCLE_O),
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
	warn		(VaadinIcons.WARNING),
	error		(VaadinIcons.CLOSE_CIRCLE),
	transfer	(VaadinIcons.LINK),
	reload		(VaadinIcons.RETWEET),
	download	(VaadinIcons.DOWNLOAD),
	upArrow		(VaadinIcons.ANGLE_UP),
	topArrow	(VaadinIcons.ANGLE_DOUBLE_UP),
	downArrow	(VaadinIcons.ANGLE_DOWN),
	bottomArrow	(VaadinIcons.ANGLE_DOUBLE_DOWN),
	wizard		(VaadinIcons.MAGIC),
	dryrun		(VaadinIcons.COG_O),
	play		(VaadinIcons.PLAY),
	messageSend	(VaadinIcons.ENVELOPE_O),
	pause		(VaadinIcons.PAUSE),
	plFlag		(I.P + "16-flags/pl.png"),
	enFlag		(I.P + "16-flags/en.png"),
	deFlag		(I.P + "16-flags/de.png"),
	menu		(VaadinIcons.MENU),
	remove		(VaadinIcons.CLOSE_CIRCLE_O),
	resize		(VaadinIcons.RESIZE_H);
	
	
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
		public static final String PH = "../common/img/hand/";
	}
}
