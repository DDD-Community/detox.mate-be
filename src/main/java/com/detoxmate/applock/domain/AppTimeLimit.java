package com.detoxmate.applock.domain;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity(name = "app_time_limits")
public class AppTimeLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
}
