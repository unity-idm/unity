/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.directoryBrowser.identities;

enum BaseColumn 
{
	entity("Identities.entity", false, false, 200), 
	type("Identities.type", true, false, 100), 
	identity("Identities.identity", true, false, 300), 
	status("Identities.status", true, false, 100), 
	local("Identities.local", true, true, 100), 
	dynamic("Identities.dynamic", true, true, 100), 
	credReq("Identities.credReq", true, true, 180), 
	target("Identities.target", true, true, 180), 
	realm("Identities.realm", true, true, 100),
	remoteIdP("Identities.remoteIdP", true, true, 200), 
	profile("Identities.profile", true, true, 100), 
	scheduledOperation("Identities.scheduledOperation", true, true, 200);

	public String captionKey;
	public boolean collapsingAllowed;
	public boolean initiallyCollapsed;
	public int defWidth;
	
	BaseColumn(String captionKey, boolean collapsingAllowed, 
			boolean initiallyCollapsed, int defWidth)
	{
		this.captionKey = captionKey;
		this.collapsingAllowed = collapsingAllowed;
		this.initiallyCollapsed = initiallyCollapsed;
		this.defWidth = defWidth;
	}
}