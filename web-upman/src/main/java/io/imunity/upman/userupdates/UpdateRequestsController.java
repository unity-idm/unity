/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.userupdates;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Update request controller
 * 
 * @author P.Piernik
 *
 */
@Component
public class UpdateRequestsController
{

	public List<UpdateRequestEntry> getUpdateRequests(String project) throws ControllerException
	{

		UpdateRequestEntry entry1 = new UpdateRequestEntry("Id1", "Update", "demo@demo.com", "Demo name",
				Arrays.asList("X Files", "Security division"), Instant.now(), new HashMap<>());

		UpdateRequestEntry entry2 = new UpdateRequestEntry("Id2", "Self sing up", "demo2@demo.com",
				"Demo2 name", Arrays.asList("Security division"), Instant.now(), new HashMap<>());

		return Arrays.asList(entry1, entry2);
	}
}
