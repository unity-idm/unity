/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.server.ClassResource;
import com.vaadin.server.Resource;

/**
 * Allows to easily get image resources.
 * @author K. Benedyczak
 */
public enum Images
{
	hInfo		(I.PH + "bullet_info64.png"),
	hKey		(I.PH + "key64.png"),
	hSettings	(I.PH + "settings64.png"),
	hAccept		(I.PH + "bullet_accept16.png"),
	hRemove		(I.PH + "bullet_deny16.png"),
	hExit		(I.PH + "arrow_right_red32.png"),
	hToAdmin	(I.PH + "user_starred32.png"),
	hToProfile	(I.PH + "user_unstarred32.png"),
	
	unknown		(I.P + "unknown.gif"),
	logout		(I.P + "logout.png"),
	add		(I.P + "add.png"),
	delete		(I.P + "delete.png"),
	edit		(I.P + "edit.png"),
	checked		(I.P + "checked.png"),
	unchecked	(I.P + "unchecked.png"),
	refresh		(I.P + "refresh.png"),
	noAuthzGrp	(I.P + "noauthzGrp.gif"),
	collapse	(I.P + "collapse.gif"),
	addSearch	(I.P + "magnifier.png"),
	ok		(I.P + "ok.gif"),
	warn		(I.P + "warn.gif"),
	error		(I.P + "error.gif"),
	addColumn	(I.P + "addColumn.png"),
	removeColumn	(I.P + "removeColumn.png"),
	folder		(I.P + "folder.gif");
	
	
	private final String classpath;
	
	private Images(String classpath)
	{
		this.classpath = classpath;
	}
	
	public Resource getResource()
	{
		return new ClassResource(classpath);
	}

	/**
	 * Trick - otherwise we won't be able to use P in enum constructor arguments
	 * @author K. Benedyczak
	 */
	private static interface I
	{
		public static final String P = "/pl/edu/icm/unity/webui/img/";
		public static final String PH = "/pl/edu/icm/unity/webui/img/hand/";
	}
}
