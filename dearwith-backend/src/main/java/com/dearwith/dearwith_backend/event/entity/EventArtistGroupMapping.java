package com.dearwith.dearwith_backend.event.entity;

import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.common.jpa.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "event_artist_group_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_artist_group",
                        columnNames = {"event_id", "artist_group_id"}
                )
        }
)
public class EventArtistGroupMapping extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ArtistGroup artistGroup;
}
