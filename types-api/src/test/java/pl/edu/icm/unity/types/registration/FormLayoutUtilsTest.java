/**********************************************************************
 *                     Copyright (c) 2018, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.registration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;

public class FormLayoutUtilsTest
{
	@Test
	public void shouldApplyRuntimeFixForMissingMigration()
	{
		// given
		ObjectNode context = Constants.MAPPER.createObjectNode();
		context.put("isOnIdpEndpoint", true);
		context.put("triggeringMode", "afterRemoteLogin");
		
		// when
		RegistrationContext registrationContext = new RegistrationContext(context);
		
		// then
		assertThat(registrationContext.triggeringMode, equalTo(TriggeringMode.afterRemoteLoginWhenUnknownUser));
	}
}
