package sartia.presentation.rest.dto;

import java.util.List;

/**
 * A page of results plus the metadata a client needs to walk through the rest.
 *
 * <p>Returning the total and the page size alongside the items means a client
 * can render page controls without a second request or a guess.
 *
 * @param items      the results on this page
 * @param page       one-based page number
 * @param pageSize   maximum items per page
 * @param totalItems total matches across all pages
 * @param totalPages number of pages available
 * @param <T>        item type
 */
public record PageDto<T>(
        List<T> items,
        int page,
        int pageSize,
        int totalItems,
        int totalPages) {

    public static <T> PageDto<T> of(List<T> items, int page, int pageSize, int totalItems) {
        int totalPages = pageSize <= 0 ? 1 : Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
        return new PageDto<>(items, page, pageSize, totalItems, totalPages);
    }
}
