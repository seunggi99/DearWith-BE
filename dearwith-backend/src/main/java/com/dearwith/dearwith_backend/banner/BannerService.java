package com.dearwith.dearwith_backend.banner;

import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {
    private final BannerRepository bannerRepository;
    private final AssetUrlService assetUrlService;

    @Transactional(readOnly = true)
    public List<BannerDto> getMainBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(b -> BannerDto.of(b, assetUrlService))
                .toList();
    }
}
