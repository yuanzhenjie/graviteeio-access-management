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
package io.gravitee.am.repository.jdbc.management.api;

import io.gravitee.am.common.utils.RandomString;
import io.gravitee.am.model.LoginAttempt;
import io.gravitee.am.repository.jdbc.exceptions.RepositoryIllegalQueryException;
import io.gravitee.am.repository.jdbc.management.AbstractJdbcRepository;
import io.gravitee.am.repository.jdbc.management.api.model.JdbcLoginAttempt;
import io.gravitee.am.repository.jdbc.management.api.spring.SpringLoginAttemptRepository;
import io.gravitee.am.repository.management.api.LoginAttemptRepository;
import io.gravitee.am.repository.management.api.search.LoginAttemptCriteria;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static java.time.ZoneOffset.UTC;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.CriteriaDefinition.from;
import static reactor.adapter.rxjava.RxJava2Adapter.*;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcLoginAttemptRepository extends AbstractJdbcRepository implements LoginAttemptRepository {
    @Autowired
    protected SpringLoginAttemptRepository loginAttemptRepository;

    protected LoginAttempt toEntity(JdbcLoginAttempt entity) {
        return mapper.map(entity, LoginAttempt.class);
    }

    protected JdbcLoginAttempt toJdbcEntity(LoginAttempt entity) {
        return mapper.map(entity, JdbcLoginAttempt.class);
    }

    @Override
    public Maybe<LoginAttempt> findByCriteria(LoginAttemptCriteria criteria) {
        LOGGER.debug("findByCriteria({})", criteria);

        Criteria whereClause = buildWhereClause(criteria);

        DatabaseClient.TypedSelectSpec<JdbcLoginAttempt> from = dbClient.select()
                .from(JdbcLoginAttempt.class)
                .page(PageRequest.of(0, 1, Sort.by("id")));

        whereClause = whereClause.and(
                where("expire_at").greaterThan(LocalDateTime.now(UTC))
                .or(where("expire_at").isNull()));
        from = from.matching(from(whereClause));

        return monoToMaybe(from.as(JdbcLoginAttempt.class).first())
                .map(this::toEntity)
                .doOnError(error -> LOGGER.error("Unable to retrieve LoginAttempt with criteria {}", criteria, error));
    }

    private Criteria buildWhereClause(LoginAttemptCriteria criteria) {
        Criteria whereClause = Criteria.empty();
        // domain
        if (criteria.domain() != null && !criteria.domain().isEmpty()) {
            whereClause = whereClause.and(where("domain").is(criteria.domain()));
        }
        // client
        if (criteria.client() != null && !criteria.client().isEmpty()) {
            whereClause = whereClause.and(where("client").is(criteria.client()));
        }
        // idp
        if (criteria.identityProvider() != null && !criteria.identityProvider().isEmpty()) {
            whereClause = whereClause.and(where("identity_provider").is(criteria.identityProvider()));
        }
        // username
        if (criteria.username() != null && !criteria.username().isEmpty()) {
            whereClause = whereClause.and(where("username").is(criteria.username()));
        }
        return whereClause;
    }

    @Override
    public Completable delete(LoginAttemptCriteria criteria) {
        LOGGER.debug("delete({})", criteria);

        Criteria whereClause = buildWhereClause(criteria);

        if (!whereClause.isEmpty()) {
            return monoToCompletable(dbClient.delete().from(JdbcLoginAttempt.class).matching(from(whereClause)).then())
                    .doOnError(error -> LOGGER.error("Unable to retrieve LoginAttempt with criteria {}", criteria, error));
        }

        throw new RepositoryIllegalQueryException("Unable to delete from LoginAttempt without criteria");
    }

    @Override
    public Maybe<LoginAttempt> findById(String id) {
        LOGGER.debug("findById({})", id);
        LocalDateTime now = LocalDateTime.now(UTC);
        return loginAttemptRepository.findById(id)
                .filter(bean -> bean.getExpireAt() == null || bean.getExpireAt().isAfter(now))
                .map(this::toEntity)
                .doOnError(error -> LOGGER.error("Unable to retrieve loginAttempt with id '{}'", id, error));
    }

    @Override
    public Single<LoginAttempt> create(LoginAttempt item) {
        item.setId(item.getId() == null ? RandomString.generate() : item.getId());
        LOGGER.debug("create LoginAttempt with id {}", item.getId());

        Mono<Integer> action = dbClient.insert()
                .into(JdbcLoginAttempt.class)
                .using(toJdbcEntity(item))
                .fetch().rowsUpdated();

        return monoToSingle(action).flatMap((i) -> this.findById(item.getId()).toSingle())
                .doOnError((error) -> LOGGER.error("unable to create loginAttempt with id {}", item.getId(), error));
    }

    @Override
    public Single<LoginAttempt> update(LoginAttempt item) {
        LOGGER.debug("update loginAttempt with id '{}'", item.getId());
        return loginAttemptRepository.save(toJdbcEntity(item))
                .map(this::toEntity)
                .doOnError(error -> LOGGER.error("unable to update loginAttempt with id {}", item.getId(), error));
    }

    @Override
    public Completable delete(String id) {
        LOGGER.debug("delete({})", id);
        return loginAttemptRepository.deleteById(id)
                .doOnError(error -> LOGGER.error("Unable to delete loginAttempt with id '{}'", id, error));
    }
}
