package com.detoxmate.applock.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity(name = "apps")
public class App {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
}
