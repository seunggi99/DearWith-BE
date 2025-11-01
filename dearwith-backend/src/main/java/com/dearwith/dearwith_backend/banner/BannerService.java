package com.dearwith.dearwith_backend.banner;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {
    private final BannerRepository bannerRepository;

    @Transactional(readOnly = true)
    public List<BannerDto> getMainBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc()
                .stream().map(BannerDto::of).toList();
    }
}
