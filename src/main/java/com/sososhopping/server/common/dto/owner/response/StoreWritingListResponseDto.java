package com.sososhopping.server.common.dto.owner.response;

import com.sososhopping.server.entity.store.Writing;
import com.sososhopping.server.entity.store.WritingType;
import lombok.*;

import java.time.format.DateTimeFormatter;

@Builder
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StoreWritingListResponseDto {
    private Long id;
    private String title;
    private String content;
    private WritingType writingType;
    private String createdAt;

    public StoreWritingListResponseDto(Writing writing) {
        this.id = writing.getId();
        this.title = writing.getTitle();
        this.content = writing.getContent();
        this.writingType = writing.getWritingType();
        this.createdAt = writing.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
    }
}
