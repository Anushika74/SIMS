package com.smartinventory.dto;

import lombok.Getter;
import lombok.Setter;

/** Payload from the "Record Wastage" form. */
@Getter
@Setter
public class WastageRequest {
    private Long productId;
    private Integer quantity;
    private String reason;   // EXPIRED | DAMAGED | SPOILED | THEFT | OTHER
    private String note;
}
