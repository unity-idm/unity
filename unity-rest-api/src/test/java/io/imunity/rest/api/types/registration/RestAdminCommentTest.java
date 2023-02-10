/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Date;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestAdminCommentTest extends RestTypeBase<RestAdminComment>
{

	@Override
	protected String getJson()
	{
		return "{\"date\":1,\"contents\":\"comment\",\"authorEntityId\":1,\"publicComment\":true}\n";
	}

	@Override
	protected RestAdminComment getObject()
	{
		return RestAdminComment.builder()
				.withAuthorEntityId(1)
				.withContents("comment")
				.withDate(new Date(1))
				.withPublicComment(true)
				.build();
	}

}
