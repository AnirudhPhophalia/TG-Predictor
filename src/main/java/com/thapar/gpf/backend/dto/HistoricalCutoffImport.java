package com.thapar.gpf.backend.dto;

import java.util.List;

public record HistoricalCutoffImport(List<Entry> historicalCutoffs) {
    public record Entry(String subjectCode, Double cutoffA, Double cutoffAMinus, Double cutoffB,
                        Double cutoffBMinus, Double cutoffC, Double cutoffD) {
    }
}
