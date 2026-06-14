package com.smartinventory.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Payload for recording a new sale (a basket of line items). */
@Getter
@Setter
public class SaleRequest {
    private String paymentMethod = "CASH";
    private List<SaleItemRequest> items = new ArrayList<>();
}
