/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.service;

import io.gravitee.am.identityprovider.api.User;
import io.gravitee.am.model.Membership;
import io.gravitee.am.model.ReferenceType;
import io.gravitee.am.model.membership.MemberType;
import io.gravitee.am.repository.management.api.search.MembershipCriteria;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

import java.util.List;
import java.util.Map;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface MembershipService {

    Maybe<Membership> findById(String id);

    Flowable<Membership> findByCriteria(ReferenceType referenceType, String referenceId, MembershipCriteria criteria);

    Single<List<Membership>> findByReference(String referenceId, ReferenceType referenceType);

    Single<List<Membership>> findByMember(String memberId, MemberType memberType);

    Single<Membership> addOrUpdate(String organizationId, Membership membership, User principal);

    Single<Map<String, Map<String, Object>>> getMetadata(List<Membership> memberships);

    Completable delete(String membershipId, User principal);

    default Single<Membership> addOrUpdate(String organizationId, Membership membership) {
        return addOrUpdate(organizationId, membership, null);
    }

    default Completable delete(String membershipId) {
        return delete(membershipId, null);
    }
}
