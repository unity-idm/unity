/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.pam;

import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;

public class TestPAM
{

	public static void main(String[] args) throws PAMException
	{
		PAM pam = new PAM("unity");
		UnixUser user = pam.authenticate("unity-test", "");
		System.out.println(unixUserInfo(user));
	}
	
	public static String unixUserInfo(UnixUser uu)
	{
		StringBuilder ret = new StringBuilder();
		ret.append("User name: " + uu.getUserName() + "\n");
		ret.append("GID: " + uu.getGID() + "\n");
		ret.append("Groups: " + uu.getGroups() + "\n");
		ret.append("Home directory: " + uu.getDir() + "\n");
		ret.append("GECOS: " + uu.getGecos() + "\n");
		ret.append("Shell: " + uu.getShell() + "\n");
		ret.append("UID: " + uu.getUID() + "\n");
		return ret.toString();
	}
}
