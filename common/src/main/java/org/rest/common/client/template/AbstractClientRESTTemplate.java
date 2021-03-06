package org.rest.common.client.template;

import static org.rest.common.util.SearchCommonUtil.SEPARATOR_AMPER;
import static org.rest.common.util.SearchCommonUtil.constructURI;

import java.util.List;

import org.apache.commons.lang3.tuple.Triple;
import org.rest.common.client.marshall.IMarshaller;
import org.rest.common.persistence.model.INameableEntity;
import org.rest.common.search.ClientOperation;
import org.rest.common.util.QueryConstants;
import org.rest.common.web.WebConstants;
import org.rest.common.web.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractClientRESTTemplate<T extends INameableEntity> implements IClientTemplate<T> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Class<T> clazz;

    @Autowired
    protected IMarshaller marshaller;

    @Autowired
    protected RestTemplate restTemplate;

    public AbstractClientRESTTemplate(final Class<T> clazzToSet) {
        super();

        clazz = clazzToSet;
    }

    // find - one

    @Override
    public final T findOne(final long id) {
        try {
            final ResponseEntity<T> response = restTemplate.exchange(getURI() + WebConstants.PATH_SEP + id, HttpMethod.GET, findRequestEntity(), clazz);
            return response.getBody();
        } catch (final HttpClientErrorException clientEx) {
            return null;
        }
    }

    @Override
    public final T findOneByURI(final String uri) {
        final ResponseEntity<T> response = restTemplate.exchange(uri, HttpMethod.GET, findRequestEntity(), clazz);
        return response.getBody();
    }

    // find one - by attributes

    @Override
    public final T findByName(final String name) {
        // return findOneByAttributes(SearchField.name.name(), name); // TODO: fix
        return findOneByURI(getURI() + "?name=" + name);
    }

    @Override
    public final T findOneByAttributes(final String... attributes) {
        final List<T> resourcesByName = findAllByURI(getURI() + QueryConstants.QUERY_PREFIX + constructURI(attributes));
        if (resourcesByName.isEmpty()) {
            return null;
        }
        Preconditions.checkState(resourcesByName.size() <= 1);
        return resourcesByName.get(0);
    }

    // find - all

    @Override
    public final List<T> findAll() {
        beforeReadOperation();
        final ResponseEntity<String> findAllResponse = restTemplate.exchange(getURI(), HttpMethod.GET, findRequestEntity(), String.class);
        final String body = findAllResponse.getBody();
        if (body == null) {
            return Lists.newArrayList();
        }
        return marshaller.decodeList(body, clazz);
    }

    @Override
    public final List<T> findAllPaginatedAndSorted(final int page, final int size, final String sortBy, final String sortOrder) {
        final ResponseEntity<String> allPaginatedAndSortedAsResponse = findAllPaginatedAndSortedAsResponse(page, size, sortBy, sortOrder);
        final String bodyAsString = allPaginatedAndSortedAsResponse.getBody();
        if (bodyAsString == null) {
            return Lists.newArrayList();
        }
        return marshaller.decodeList(bodyAsString, clazz);
    }

    @Override
    public final List<T> findAllSorted(final String sortBy, final String sortOrder) {
        beforeReadOperation();
        final String uri = getURI() + QueryConstants.Q_SORT_BY + sortBy + QueryConstants.S_ORDER + sortOrder;
        final ResponseEntity<String> findAllResponse = restTemplate.exchange(uri, HttpMethod.GET, findRequestEntity(), String.class);
        final String body = findAllResponse.getBody();
        if (body == null) {
            return Lists.newArrayList();
        }
        return marshaller.decodeList(body, clazz);
    }

    @Override
    public final List<T> findAllPaginated(final int page, final int size) {
        beforeReadOperation();
        final StringBuilder uri = new StringBuilder(getURI());
        uri.append(QueryConstants.QUESTIONMARK);
        uri.append("page=");
        uri.append(page);
        uri.append(SEPARATOR_AMPER);
        uri.append("size=");
        uri.append(size);
        final ResponseEntity<List> findAllResponse = restTemplate.exchange(uri.toString(), HttpMethod.GET, findRequestEntity(), List.class);
        final List<T> body = findAllResponse.getBody();
        if (body == null) {
            return Lists.newArrayList();
        }
        return body;
    }

    @Override
    public final List<T> findAllByAttributes(final String... attributes) {
        final String uri = getURI() + QueryConstants.QUERY_PREFIX + constructURI(attributes);
        final List<T> resourcesByAttributes = findAllByURI(uri);
        return resourcesByAttributes;
    }

    @Override
    public final List<T> findAllByURI(final String uri) {
        final ResponseEntity<List> response = restTemplate.exchange(uri, HttpMethod.GET, findRequestEntity(), List.class);
        final List<T> body = response.getBody();
        if (body == null) {
            return Lists.newArrayList();
        }
        return body;
    }

    final ResponseEntity<String> findAllPaginatedAndSortedAsResponse(final int page, final int size, final String sortBy, final String sortOrder) {
        beforeReadOperation();
        final StringBuilder uri = new StringBuilder(getURI());
        uri.append(QueryConstants.QUESTIONMARK);
        uri.append("page=");
        uri.append(page);
        uri.append(SEPARATOR_AMPER);
        uri.append("size=");
        uri.append(size);
        Preconditions.checkState(!(sortBy == null && sortOrder != null));
        if (sortBy != null) {
            uri.append(SEPARATOR_AMPER);
            uri.append(QueryConstants.SORT_BY + "=");
            uri.append(sortBy);
        }
        if (sortOrder != null) {
            uri.append(SEPARATOR_AMPER);
            uri.append(QueryConstants.SORT_ORDER + "=");
            uri.append(sortOrder);
        }

        return restTemplate.exchange(uri.toString(), HttpMethod.GET, findRequestEntity(), String.class);
    }

    // create

    @Override
    public final T create(final T resource) {
        final String locationOfCreatedResource = createAsURI(resource);

        return findOneByURI(locationOfCreatedResource);
    }

    @Override
    public final String createAsURI(final T resource) {
        givenAuthenticated(null, null);
        final ResponseEntity<Void> responseEntity = restTemplate.exchange(getURI(), HttpMethod.POST, new HttpEntity<T>(resource, writeHeaders()), Void.class);

        final String locationOfCreatedResource = responseEntity.getHeaders().getLocation().toString();
        Preconditions.checkNotNull(locationOfCreatedResource);

        return locationOfCreatedResource;
    }

    // update

    @Override
    public final void update(final T resource) {
        givenAuthenticated(null, null);
        final ResponseEntity<T> responseEntity = restTemplate.exchange(getURI(), HttpMethod.PUT, new HttpEntity<T>(resource, writeHeaders()), clazz);
        Preconditions.checkState(responseEntity.getStatusCode().value() == 200);
    }

    // delete

    @Override
    public final void delete(final long id) {
        final ResponseEntity<Object> deleteResourceResponse = restTemplate.exchange(getURI() + WebConstants.PATH_SEP + id, HttpMethod.DELETE, null, null);

        Preconditions.checkState(deleteResourceResponse.getStatusCode().value() == 204);
    }

    @Override
    public final void deleteAll() {
        throw new UnsupportedOperationException();
    }

    // search

    @Override
    public final List<T> search(final Triple<String, ClientOperation, String>... constraints) {
        throw new UnsupportedOperationException();
    }

    // count

    @Override
    public final long count() {
        throw new UnsupportedOperationException();
    }

    // util

    protected final HttpEntity<Void> findRequestEntity() {
        return new HttpEntity<Void>(findHeaders());
    }

    // template method

    @Override
    public final IClientTemplate<T> givenAuthenticated(final String username, final String password) {
        if (isBasicAuth()) {
            basicAuth(username, password);
        } else {
            digestAuth(username, password);
        }

        return this;
    }

    protected boolean isBasicAuth() {
        return true;
    }

    protected abstract void basicAuth(final String username, final String password);

    @SuppressWarnings("unused")
    protected final void digestAuth(final String username, final String password) {
        throw new UnsupportedOperationException();
    }

    /**
     * - this is a hook that executes before read operations, in order to allow custom security work to happen for read operations; similar to: AbstractRESTTemplate.findRequest
     */
    protected void beforeReadOperation() {
        //
    }

    /**
     * - note: hook to be able to customize the find headers if needed
     */
    protected HttpHeaders findHeaders() {
        return HeaderUtil.createAcceptHeaders(marshaller);
    }

    /**
     * - note: hook to be able to customize the write headers if needed
     */
    protected HttpHeaders writeHeaders() {
        return HeaderUtil.createContentTypeHeaders(marshaller);
    }

}
