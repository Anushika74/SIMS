package com.smartinventory.dto;

import lombok.Getter;
import lombok.Setter;

/** One line submitted from the "Record Sale" form. */
@Getter
@Setter
public class SaleItemRequest {
    private Long productId;
    private Integer quantity;
}
