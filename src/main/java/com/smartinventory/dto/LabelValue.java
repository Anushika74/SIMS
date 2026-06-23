package com.smartinventory.dto;

import java.math.BigDecimal;

/**
 * Generic aggregation projection used by wastage reports/charts:
 * a label (reason / month / product), the total loss value, and total quantity.
 */
public interface LabelValue {
    String getLabel();
    BigDecimal getTotal();   // loss amount
    Long getQty();           // total quantity
}
