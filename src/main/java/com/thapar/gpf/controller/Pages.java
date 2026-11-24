package com.thapar.gpf.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;

/**
 * Pagination for every list page. Both methods return Spring Data's {@link Page},
 * which the shared "pager" fragment in layout.html turns into Previous / 1 2 3 / Next links.
 */
final class Pages {

    static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private Pages() {
    }

    /**
     * Database pagination: the query runs with LIMIT/OFFSET, so only one page of rows is loaded.
     * A page number past the end (an old link, a hand-typed URL) shows the last page instead.
     */
    static <T> Page<T> fetch(int page, int size, Sort sort, Function<Pageable, Page<T>> query) {
        Page<T> result = query.apply(PageRequest.of(Math.max(page, 0), size, sort));
        if (result.getTotalPages() > 0 && result.getNumber() >= result.getTotalPages()) {
            result = query.apply(PageRequest.of(result.getTotalPages() - 1, size, sort));
        }
        return result;
    }

    /** In-memory pagination for lists that are filtered or computed in Java first (search results, statistics). */
    static <T> Page<T> of(List<T> all, int page, int size) {
        int last = Math.max(0, (all.size() - 1) / size);
        int number = Math.min(Math.max(page, 0), last);
        int from = number * size;
        return new PageImpl<>(all.subList(from, Math.min(from + size, all.size())),
                PageRequest.of(number, size), all.size());
    }
}
