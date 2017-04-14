/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

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

	stderror64	(I.P + "64/error.png"),
	stdwarn64	(I.P + "64/warning.png"),
	stdinfo64	(I.P + "64/info.png"),
	
	exit32		(I.P + "32/exit.png"),
	error32		(I.P + "32/error.png"),
	warn32		(I.P + "32/warn.png"),
	info32		(I.P + "32/information.png"),
	toAdmin32	(I.P + "32/manager.png"),
	toProfile32	(I.P + "32/personal.png"),
	support32	(I.P + "32/support.png"),
	ok32		(I.P + "32/ok.png"),

	
	refresh		(I.P + "16/reload.png"),
	userMagnifier	(I.P + "16/search.png"),
	folder		(I.P + "16/folder.png"),
	add		(I.P + "16/add.png"),
	addIdentity	(I.P + "16/identity_add.png"),
	addEntity	(I.P + "16/entity_add.png"),
	addFolder	(I.P + "16/folder_add.png"),
	delete		(I.P + "16/remove.png"),
	deleteFolder	(I.P + "16/folder_delete.png"),
	deleteEntity	(I.P + "16/entity_delete.png"),
	deleteIdentity	(I.P + "16/identity_delete.png"),
	edit		(I.P + "16/edit.png"),
	copy		(I.P + "16/copy.png"),
	editUser	(I.P + "16/user_edit.png"),
	editFolder	(I.P + "16/folder_edit.png"),
	ok		(I.P + "16/ok.png"),
	save		(I.P + "16/save.png"),
	trashBin	(I.P + "16/trash.png"),
	addFilter	(I.P + "16/search.png"),
	noAuthzGrp	(I.P + "16/folder_locked.png"),
	collapse	(I.P + "16/collapse.png"),
	expand		(I.P + "16/expand.png"),
	addColumn	(I.P + "16/column_add.png"),
	removeColumn	(I.P + "16/column_delete.png"),
	key		(I.P + "16/key.png"),
	attributes	(I.P + "16/three_tags.png"),
	warn		(I.P + "16/warn.png"),
	error		(I.P + "16/error.png"),
	zoomin          (I.P + "16/zoom_in.png"),
	zoomout         (I.P + "16/zoom_out.png"),
	transfer        (I.P + "16/transfer.png"),
	upArrow         (I.P + "16/up.png"),
	topArrow	(I.P + "16/topArrow.png"),
	downArrow       (I.P + "16/down.png"),
	bottomArrow	(I.P + "16/bottomArrow.png"),
	wizard		(I.P + "16/wizard.png"),
	dryrun		(I.P + "16/dryrun.png"),
	play		(I.P + "16/play.png"),
	messageSend	(I.P + "16/message-go.png"),
	pause		(I.P + "16/pause.png"),
	cross		(I.P + "16/cross.png"),
	help       	(I.P + "16/help.png"),
	confirm		(I.P + "16/confirm.png"),
	plFlag		(I.P + "16-flags/pl.png"),
	enFlag		(I.P + "16-flags/en.png"),
	deFlag		(I.P + "16-flags/de.png"),
	;
	
	
	private final String classpath;
	
	private Images(String classpath)
	{
		this.classpath = classpath;
	}
	
	public Resource getResource()
	{
		return new ThemeResource(classpath);
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
