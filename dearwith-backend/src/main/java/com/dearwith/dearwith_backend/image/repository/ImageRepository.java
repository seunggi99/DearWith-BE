package com.dearwith.dearwith_backend.image.repository;

import com.dearwith.dearwith_backend.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
