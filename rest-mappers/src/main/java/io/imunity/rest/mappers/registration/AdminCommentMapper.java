/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import pl.edu.icm.unity.types.registration.AdminComment;

import io.imunity.rest.api.types.registration.RestAdminComment;

public class AdminCommentMapper
{
	public static RestAdminComment map(AdminComment adminComment)
	{
		return RestAdminComment.builder()
				.withAuthorEntityId(adminComment.getAuthorEntityId())
				.withContents(adminComment.getContents())
				.withDate(adminComment.getDate())
				.withPublicComment(adminComment.isPublicComment())
				.build();
	}

	public static AdminComment map(RestAdminComment restAdminComment)
	{
		AdminComment adminComment = new AdminComment(restAdminComment.contents, restAdminComment.authorEntityId,
				restAdminComment.publicComment);
		adminComment.setDate(restAdminComment.date);
		return adminComment;
	}
}
