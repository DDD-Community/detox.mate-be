package com.detoxmate.applock.service;

import com.detoxmate.applock.repository.AppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppService {

    private final AppRepository appRepository;
}
