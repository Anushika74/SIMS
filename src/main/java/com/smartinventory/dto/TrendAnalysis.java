package com.smartinventory.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Feature 3 — Sales Trend Analysis: the historical daily series plus a
 * forecast for the next period and human-readable insights.
 */
@Getter
@Setter
public class TrendAnalysis {
    private List<String> labels = new ArrayList<>();        // historical day labels
    private List<Double> values = new ArrayList<>();        // historical revenue per day
    private List<String> forecastLabels = new ArrayList<>();// future day labels
    private List<Double> forecastValues = new ArrayList<>();// forecast revenue per day

    private double weekdayAverage;
    private double weekendAverage;
    private double growthRatePct;          // slope-based growth over the window
    private List<String> insights = new ArrayList<>();
    private String source = "rule-based";
}
