package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.event.repository.EventImageMappingRepository;
import com.dearwith.dearwith_backend.event.repository.EventRepository;
import com.dearwith.dearwith_backend.image.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventImageService {
    private final EventRepository eventRepository;
    private final ImageRepository imageRepository;
    private final EventImageMappingRepository eventImageRepository;


}
