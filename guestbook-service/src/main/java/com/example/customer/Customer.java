package com.example.customer;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * @author Deshan Chathusanka
 * @since 30 May 2020
 * domain object to represent customer
 */
@Entity
@Data
public class Customer
{
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )/*Assign auto generated value*/
    private Long id;

    private String firstName;
    private String lastName;
    private String address;
}
