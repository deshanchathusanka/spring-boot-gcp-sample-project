package com.example.guestbook;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Deshan Chathusanka
 * @since 5 Jun 2020
 */
@RepositoryRestResource
public interface GuestbookMessageRepository extends 
	PagingAndSortingRepository<GuestbookMessage, Long> {
}

