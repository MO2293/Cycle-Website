package com.cyclehaven.entity;

/**
 * Product categories, matching the filter options offered in the storefront.
 * Stored as a string in the database so adding a value never shifts existing rows.
 */
public enum Category {
    TANDEM,
    BMX,
    MOUNTAIN,
    ELECTRIC,
    ROAD,
    KIDS
}
