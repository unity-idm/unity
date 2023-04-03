/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.types.registration.AdminComment;

public class AdminCommentMapper
{
	public static DBAdminComment map(AdminComment adminComment)
	{
		return DBAdminComment.builder()
				.withAuthorEntityId(adminComment.getAuthorEntityId())
				.withContents(adminComment.getContents())
				.withDate(adminComment.getDate())
				.withPublicComment(adminComment.isPublicComment())
				.build();
	}

	public static AdminComment map(DBAdminComment restAdminComment)
	{
		AdminComment adminComment = new AdminComment(restAdminComment.contents, restAdminComment.authorEntityId,
				restAdminComment.publicComment);
		adminComment.setDate(restAdminComment.date);
		return adminComment;
	}
}
