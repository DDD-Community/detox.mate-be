package com.detoxmate.transferminute.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transfer_phrase")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TransferPhrase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_phrase", nullable = false)
    private String transferPhrase;

}
