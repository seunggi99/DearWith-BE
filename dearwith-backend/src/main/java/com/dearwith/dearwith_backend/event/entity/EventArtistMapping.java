package com.dearwith.dearwith_backend.event.entity;

import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.common.jpa.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "event_artist_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_artist",
                        columnNames = {"event_id", "artist_id"}
                )
        }
)
public class EventArtistMapping extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Event event;
}
