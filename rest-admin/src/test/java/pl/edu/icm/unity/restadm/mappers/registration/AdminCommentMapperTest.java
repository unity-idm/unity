/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.Date;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.registration.RestAdminComment;
import pl.edu.icm.unity.base.registration.AdminComment;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;

public class AdminCommentMapperTest extends MapperTestBase<AdminComment, RestAdminComment>
{

	@Override
	protected AdminComment getFullAPIObject()
	{
		AdminComment adminComment = new AdminComment("comment", 1, true);
		adminComment.setDate(new Date(1));
		return adminComment;
	}

	@Override
	protected RestAdminComment getFullRestObject()
	{
		return RestAdminComment.builder()
				.withAuthorEntityId(1)
				.withContents("comment")
				.withDate(new Date(1))
				.withPublicComment(true)
				.build();
	}

	@Override
	protected Pair<Function<AdminComment, RestAdminComment>, Function<RestAdminComment, AdminComment>> getMapper()
	{
		return Pair.of(AdminCommentMapper::map, AdminCommentMapper::map);
	}

}
