package com.dequeue.vendor.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String country;
}
