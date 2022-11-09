/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import javax.ws.rs.core.Response.Status;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Test;

import eu.unicore.util.httpclient.HttpResponseHandler;
import pl.edu.icm.unity.engine.bulkops.action.RemoveEntityActionFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class TestBulkProcessing extends RESTAdminTestBase
{
	@Test
	public void submittedProcessingActionIsExecuted() throws Exception
	{
		IdentityParam identityParam = new IdentityParam(UsernameIdentity.ID, "user-to-remove");	
		idsMan.addEntity(identityParam, CRED_REQ_PASS, EntityState.valid);
		
		HttpPost post = new HttpPost("/restadm/v1/bulkProcessing/instant?timeout=20");
		TranslationRule param = new TranslationRule(
				"(idsByType contains 'userName') && (idsByType['userName'] contains 'user-to-remove')", 
				new TranslationAction(RemoveEntityActionFactory.NAME));
		String jsonform = m.writeValueAsString(param);
		System.out.println("Request to be sent:\n" + jsonform);
		post.setEntity(new StringEntity(jsonform, ContentType.APPLICATION_JSON));
		ClassicHttpResponse responsePost = client.execute(host, post, localcontext, HttpResponseHandler.INSTANCE);
		String contents = EntityUtils.toString(responsePost.getEntity());
		assertThat(responsePost.getCode()).as(contents).isEqualTo(Status.OK.getStatusCode());

		assertThat(contents).isEqualTo("sync");
		Throwable error = catchThrowable(() -> idsMan.getEntity(new EntityParam(identityParam)));
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
}
