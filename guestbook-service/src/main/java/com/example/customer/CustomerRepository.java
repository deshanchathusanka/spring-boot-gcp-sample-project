package com.example.customer;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * @author Deshan Chathusanka
 * @since 30 May 2020
 * This repository is used to direct Spring MVC to create REST endpoint
 */
@RepositoryRestResource( collectionResourceRel = "customer", path = "customer" )/*default path is customers if not specified*/
public interface CustomerRepository extends
        PagingAndSortingRepository<Customer,Long>
{
    List<Customer> findByLastName( @Param( "lastName" ) String lastName );
}
